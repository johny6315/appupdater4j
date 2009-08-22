package org.gdteam.appupdater4j.ui.dialog;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.gdteam.appupdater4j.UpdateController;
import org.gdteam.appupdater4j.UpdateControllerListener;
import org.gdteam.appupdater4j.model.ApplicationVersion;

public class UpdateDialog extends JFrame implements UpdateController {
    
    public static final int UPLOAD_ICON_COLUMN = 0;
    public static final int UPLOAD_NAME_COLUMN = 1;
    public static final int UPLOAD_SIZE_COLUMN = 2;
    public static final int UPLOAD_STATE_COLUMN = 3;
    
    public static final Icon ICON_OK = new ImageIcon(UpdateDialog.class.getClassLoader().getResource("checkmark-16.png"));
    public static final Icon ICON_FAILED = new ImageIcon(UpdateDialog.class.getClassLoader().getResource("cancel-16.png"));
    
    private JButton installButton, continueButton, cancelButton;
    private JPanel validatePane;
    private JTable updateTable;
    
    private List<ApplicationVersion> versionList;
    
    private Map<ApplicationVersion, Integer> appVersionTableIndexes = new HashMap<ApplicationVersion, Integer>();
    
    private long currentFileSize = 0;
    private long currentDownloadedSize = 0;
    private long elapsedTime = 0;
    
    private List<UpdateControllerListener> listenerList = new ArrayList<UpdateControllerListener>();
    
    private Timer timer = new Timer();
    private boolean downloading = false;
    private DownloadTask elapsedTimeTask = new DownloadTask();


    public UpdateDialog() {
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        this.setPreferredSize(new Dimension(512, 584));
        
        JPanel mainPane = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(20, 20, 5, 20);
        
        mainPane.add(new JLabel(new ImageIcon(this.getClass().getClassLoader().getResource("refresh-64.png"))), gbc);
        
        gbc.gridx++;
        gbc.weighty = 0;
        gbc.weightx = 1;
        gbc.gridheight = 1;
        gbc.insets = new Insets(20, 0, 5, 20);
        
        JLabel title = new JLabel("Nouvelle(s) version(s) disponible(s)");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        
        mainPane.add(title, gbc);
        
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 0, 10, 20);
        
        mainPane.add(new JLabel("<html>L'installation de ce logiciel peut prendre quelques temps. Si vous n'etes pas pret a l'effectuer immediatement, vous pouvez selectionner le bouton \"Plus tard\".</html>"), gbc);
        
        this.getContentPane().add(mainPane);
        
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 20, 0, 20);
        
        this.updateTable = new JTable(new DefaultTableModel(0, 4){

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Icon.class;
                } else {
                    return super.getColumnClass(columnIndex);
                }   
            }
        });
        
        TableColumnModel columnModel = this.updateTable.getColumnModel();
        
        TableColumn stateColumn = columnModel.getColumn(0);
        stateColumn.setHeaderValue("");
        stateColumn.setMaxWidth(20);
        stateColumn.setMinWidth(20);
        
        TableColumn versionColumn = columnModel.getColumn(1);
        versionColumn.setHeaderValue("Version");
        
        TableColumn sizeColumn = columnModel.getColumn(2);
        sizeColumn.setHeaderValue("Taille");
        
        TableColumn downloadColumn = columnModel.getColumn(3);
        downloadColumn.setHeaderValue("Telechargement/Installation");
        downloadColumn.setCellRenderer(new UpdateActionCellRenderer());
        
        JTextArea description = new JTextArea();
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(this.updateTable), new JScrollPane(description));
        
        mainPane.add(splitPane, gbc);
        
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 20, 20, 20);
        
        JLabel remark = new JLabel("<html>Remarque : l’utilisation de ce logiciel est soumise a l’acceptation du ou des contrats de licence de logiciel fourni(s) avec le logiciel en cours de mise a jour.</html>");
        
        mainPane.add(remark, gbc);
        
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 20, 20, 20);
        
        this.installButton = new JButton("Installer");
        this.installButton.setSelected(true);
        this.installButton.requestFocus();
        this.cancelButton = new JButton("Plus tard");
        this.continueButton = new JButton("Lancer");
        
        this.validatePane = new JPanel(new CardLayout());
        
        this.validatePane.add(this.installButton, "install");
        this.validatePane.add(this.continueButton, "continue");
        ((CardLayout) this.validatePane.getLayout()).show(this.validatePane, "install");
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.add(cancelButton);
        buttonPane.add(this.validatePane);
        
        this.installButton.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                for (UpdateControllerListener listener : listenerList) {
                    listener.startUpdate(UpdateDialog.this, versionList);
                    installButton.setEnabled(false);
                }
            }
        });
        
        mainPane.add(buttonPane, gbc);
        
        this.pack();
        
        //Resize table header sizes
        int updateTableWidth = this.updateTable.getWidth();
        int downloadWidth = ((updateTableWidth - 20) * 50 ) / 100;
        int versionWidth = ((updateTableWidth - 20) * 33 ) / 100;
        int sizeWidth = ((updateTableWidth - 20) * 17 ) / 100;
        
        versionColumn.setPreferredWidth(versionWidth);
        sizeColumn.setPreferredWidth(sizeWidth);
        downloadColumn.setPreferredWidth(downloadWidth);
        this.setLocationRelativeTo(null);
        
        splitPane.setDividerLocation(0.3);
        
        this.timer.schedule(this.elapsedTimeTask, 0, 1000);
    }

    public void addUpdateControllerListener(UpdateControllerListener listener) {
        this.listenerList.add(listener);
    }

    public void displayController() {
        this.setVisible(true);
    }

    public void removeUpdateControllerListener(UpdateControllerListener listener) {
        this.listenerList.remove(listener);
    }

    public void setVersionToInstall(List<ApplicationVersion> versionList) {
        
        this.versionList = versionList;
        this.appVersionTableIndexes.clear();
        
        DefaultTableModel model = (DefaultTableModel) this.updateTable.getModel();
        int rowCount = model.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            model.removeRow(i);
        }
        
        int index = 0;
        
        for (ApplicationVersion applicationVersion : versionList) {
            
            Long fileSize = applicationVersion.getFileSize();
            String fileSizeToDisplay = "";
            if (fileSize != null) {
                fileSizeToDisplay = this.getHumanFriendlySize(fileSize.doubleValue());
            }
            
            UpdateAction updateAction = new UpdateAction();
            updateAction.setProgression(0);
            updateAction.setDescription("");
            
            Object[] data = {"",
                             applicationVersion.getName(),
                             fileSizeToDisplay,
                             updateAction};
            
            model.addRow(data);
            
            this.appVersionTableIndexes.put(applicationVersion, Integer.valueOf(index));
            
            index++;
        }
        model.fireTableDataChanged();
    }
    
    private String getHumanFriendlySize(double length) {
        String[] sizes = {"B", "KB", "MB", "GB"};
        return getHumanFriendlySizeRec(length, sizes, 0);
    }
    
    private String getHumanFriendlySizeRec(double length, String[] sizes, int index) {
        DecimalFormat df = new DecimalFormat("#.#");
        if (length <= 1024) {
            return df.format(length) + " " + sizes[index];
        } else {
            //length > 1024
            double newLength = length / 1024;
            if (sizes.length > index) {
                //there are available sizes... Continue
                return getHumanFriendlySizeRec(newLength, sizes, index + 1);
            } else {
                //No avaialabe size.
                return df.format(newLength) + " " + sizes[index];
            }
        }
    }
    
    private UpdateAction getUploadActionData(ApplicationVersion appVersion) {
        return (UpdateAction) this.updateTable.getModel().getValueAt(this.appVersionTableIndexes.get(appVersion), UPLOAD_STATE_COLUMN);
    }
    
    private String getCountDownText(long duration) {
        long hour = duration / 3600;
        long min = (duration - (hour * 3600)) / 60;
        long sec = duration - (hour * 3600) - (min * 60);

        StringBuilder res = new StringBuilder();
        if (hour > 0){
            res.append(hour).append(":");
        }
        
        if (min < 10){
            res.append("0").append(min).append(":");
        } else {
            res.append(min).append(":");
        }
        
        if (sec < 10){
            res.append("0").append(sec).append(" s");
        } else {
            res.append(sec).append(" s");
        }
        
        return res.toString();
    }

    private class DownloadTask extends TimerTask {

        private ApplicationVersion appVersion;
        
        @Override
        public void run() {
            if (elapsedTime > 0){
                elapsedTime--;
                
                UpdateAction action = getUploadActionData(appVersion);
                action.setDescription(getCountDownText(elapsedTime));
                ((DefaultTableModel) updateTable.getModel()).fireTableDataChanged();
            }
        }
        
        
    }

    public void downloadDone(ApplicationVersion applicationVersion, File dest) {
        this.downloading = false;
        
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setDescription("Telecharge");
        action.setProgression(100);
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }

    public void downloadFailed(ApplicationVersion applicationVersion) {
        this.downloading = false;
        
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setDescription("Erreur");
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }

    public void downloadStarted(ApplicationVersion applicationVersion, long size) {
        this.downloading = true;
        
        this.currentFileSize = size;
        this.currentDownloadedSize = 0;
        
        this.elapsedTimeTask.appVersion = applicationVersion;
        
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setDescription("Telechargement");
        action.setProgression(0);
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }

    public void downloadedDataChanged(ApplicationVersion applicationVersion, long size) {
        this.currentDownloadedSize = size;
        
        double progressValue = (Long.valueOf(this.currentDownloadedSize).doubleValue() / Long.valueOf(this.currentFileSize).doubleValue()) * Double.valueOf(100).doubleValue();
        
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setProgression(Double.valueOf(progressValue).intValue());
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }

    public void flowSizeChanged(ApplicationVersion applicationVersion, long size) {
        this.elapsedTime = (this.currentFileSize - this.currentDownloadedSize) / size;
        
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setDescription(this.getCountDownText(elapsedTime));
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }

    public void installationEnded(ApplicationVersion applicationVersion) {
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setDescription("Installe");
        action.setIndeterminate(false);
        
        int rowIndex = this.appVersionTableIndexes.get(applicationVersion);
        this.updateTable.getModel().setValueAt(ICON_OK, rowIndex, UPLOAD_ICON_COLUMN);
        
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }

    public void installationFailed(ApplicationVersion applicationVersion, Exception e) {
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setDescription("Erreur");
        action.setIndeterminate(false);
        
        int rowIndex = this.appVersionTableIndexes.get(applicationVersion);
        this.updateTable.getModel().setValueAt(ICON_FAILED, rowIndex, UPLOAD_ICON_COLUMN);
        
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }

    public void installationStarted(ApplicationVersion applicationVersion, String basedir) {
        UpdateAction action = this.getUploadActionData(applicationVersion);
        action.setDescription("Installation");
        action.setIndeterminate(true);
        ((DefaultTableModel) this.updateTable.getModel()).fireTableDataChanged();
    }
}