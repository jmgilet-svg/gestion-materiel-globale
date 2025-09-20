package com.materiel.suite.client.ui.common;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Aperçu simple avant envoi des emails générés. */
public class EmailPreviewDialog extends JDialog {
  public record EmailJob(String to, String cc, String subject, String body, List<File> attachments){
    public EmailJob{
      attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
  }

  private final List<EmailJob> jobs;
  private int index = 0;
  private final JLabel toLabel = new JLabel();
  private final JLabel ccLabel = new JLabel();
  private final JTextField subjectField = new JTextField();
  private final JTextArea bodyArea = new JTextArea();
  private final DefaultListModel<File> attachmentsModel = new DefaultListModel<>();
  private final JList<File> attachmentsList = new JList<>(attachmentsModel);
  private boolean approved;

  public EmailPreviewDialog(Window owner, List<EmailJob> jobs){
    super(owner, "Aperçu des emails", ModalityType.DOCUMENT_MODAL);
    if (jobs == null || jobs.isEmpty()){
      throw new IllegalArgumentException("jobs is empty");
    }
    this.jobs = new ArrayList<>(jobs);

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setSize(720, 520);
    setLocationRelativeTo(owner);
    setLayout(new BorderLayout(8, 8));

    JPanel header = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 8, 6, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;

    int row = 0;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    header.add(new JLabel("À"), gc);
    gc.gridx = 1; gc.weightx = 1;
    header.add(toLabel, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    header.add(new JLabel("Cc"), gc);
    gc.gridx = 1; gc.weightx = 1;
    header.add(ccLabel, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    header.add(new JLabel("Sujet"), gc);
    gc.gridx = 1; gc.weightx = 1;
    header.add(subjectField, gc);
    subjectField.setEditable(false);

    add(header, BorderLayout.NORTH);

    bodyArea.setEditable(false);
    bodyArea.setLineWrap(true);
    bodyArea.setWrapStyleWord(true);
    JScrollPane bodyScroll = new JScrollPane(bodyArea);

    attachmentsList.setVisibleRowCount(6);
    JScrollPane attachmentsScroll = new JScrollPane(attachmentsList);
    JPanel right = new JPanel(new BorderLayout());
    right.add(new JLabel("Pièces jointes"), BorderLayout.NORTH);
    right.add(attachmentsScroll, BorderLayout.CENTER);

    JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bodyScroll, right);
    center.setResizeWeight(0.7);
    add(center, BorderLayout.CENTER);

    JButton previous = new JButton("◀ Précédent");
    JButton next = new JButton("Suivant ▶");
    JLabel counter = new JLabel();
    JButton send = new JButton("Envoyer");
    JButton cancel = new JButton("Annuler");

    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    footer.add(previous);
    footer.add(next);
    footer.add(counter);
    footer.add(Box.createHorizontalStrut(16));
    footer.add(send);
    footer.add(cancel);
    add(footer, BorderLayout.SOUTH);

    Runnable refresh = () -> {
      EmailJob job = this.jobs.get(index);
      toLabel.setText(job.to());
      ccLabel.setText(job.cc() == null ? "" : job.cc());
      subjectField.setText(job.subject());
      bodyArea.setText(job.body());
      attachmentsModel.clear();
      for (File attachment : job.attachments()){
        attachmentsModel.addElement(attachment);
      }
      counter.setText((index + 1) + " / " + this.jobs.size());
      previous.setEnabled(index > 0);
      next.setEnabled(index < this.jobs.size() - 1);
    };

    previous.addActionListener(e -> {
      if (index > 0){
        index--;
        refresh.run();
      }
    });
    next.addActionListener(e -> {
      if (index < jobs.size() - 1){
        index++;
        refresh.run();
      }
    });
    cancel.addActionListener(e -> {
      approved = false;
      dispose();
    });
    send.addActionListener(e -> {
      approved = true;
      dispose();
    });

    refresh.run();
  }

  public boolean isApproved(){
    return approved;
  }
}
