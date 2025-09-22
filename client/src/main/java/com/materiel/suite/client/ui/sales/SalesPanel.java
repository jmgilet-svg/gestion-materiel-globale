package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.model.InvoiceV2;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.service.AgencyConfigGateway;
import com.materiel.suite.client.service.MailService;
import com.materiel.suite.client.service.SalesService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.sales.pdf.PdfMini;
import com.materiel.suite.client.ui.sales.xls.ExcelXml;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ecran Devis/Factures (édition inline) avec filtrage agence. */
public class SalesPanel extends JPanel {
  private final JTabbedPane tabs = new JTabbedPane();
  // Devis
  private final JTable quotesTable = new JTable();
  private final QuoteTableModel quotesModel = new QuoteTableModel();
  private final JButton addQuote = new JButton("Nouveau devis");
  private final JButton delQuote = new JButton("Supprimer");
  private final JButton saveQuote = new JButton("Enregistrer");
  private final JButton reloadQuote = new JButton("Recharger");
  private final JButton pdfQuote = new JButton("Exporter PDF");
  private final JButton pdfQuoteDetail = new JButton("PDF détaillé");
  private final JButton quoteToInvoice = new JButton("Générer facture");
  private final JButton quoteToInvoicesMulti = new JButton("Générer factures (sélection)");
  private final JButton csvQuote = new JButton("Exporter CSV");
  private final JButton xlsQuote = new JButton("Exporter Excel");
  private final JButton mailQuote = new JButton("Envoyer PDF…");
  private final JButton mailQuoteInsertLines = new JButton("Insérer lignes (HTML)");
  private final JTextField searchQuote = new JTextField(18);
  private final JLabel searchQuoteLbl = new JLabel("Recherche:");
  private TableRowSorter<QuoteTableModel> quoteSorter;
  // Factures
  private final JTable invoicesTable = new JTable();
  private final InvoiceTableModel invoicesModel = new InvoiceTableModel();
  private final JButton addInvoice = new JButton("Nouvelle facture");
  private final JButton delInvoice = new JButton("Supprimer");
  private final JButton saveInvoice = new JButton("Enregistrer");
  private final JButton reloadInvoice = new JButton("Recharger");
  private final JButton pdfInvoice = new JButton("Exporter PDF");
  private final JButton pdfInvoiceDetail = new JButton("PDF détaillé");
  private final JButton csvInvoice = new JButton("Exporter CSV");
  private final JButton xlsInvoice = new JButton("Exporter Excel");
  private final JButton mailInvoice = new JButton("Envoyer PDF…");
  private final JButton mailInvoiceInsertLines = new JButton("Insérer lignes (HTML)");
  private final JTextField searchInvoice = new JTextField(18);
  private final JLabel searchInvoiceLbl = new JLabel("Recherche:");
  private TableRowSorter<InvoiceTableModel> invoiceSorter;

  public SalesPanel(){
    super(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    // --- Devis tab ---
    JPanel quotesPane = new JPanel(new BorderLayout());
    JPanel qbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    qbar.add(addQuote);
    qbar.add(delQuote);
    qbar.add(saveQuote);
    qbar.add(reloadQuote);
    qbar.add(pdfQuote);
    qbar.add(pdfQuoteDetail);
    qbar.add(mailQuote);
    qbar.add(mailQuoteInsertLines);
    qbar.add(quoteToInvoice);
    qbar.add(quoteToInvoicesMulti);
    qbar.add(csvQuote);
    qbar.add(xlsQuote);
    qbar.add(Box.createHorizontalStrut(12));
    qbar.add(searchQuoteLbl);
    qbar.add(searchQuote);
    quotesTable.setModel(quotesModel);
    quotesTable.setFillsViewportHeight(true);
    quotesTable.setRowHeight(24);
    quotesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    quotesTable.setDefaultRenderer(LocalDate.class, new LocalDateRenderer());
    quotesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    quoteSorter = new TableRowSorter<>(quotesModel);
    quotesTable.setRowSorter(quoteSorter);
    quotesPane.add(qbar, BorderLayout.NORTH);
    quotesPane.add(new JScrollPane(quotesTable), BorderLayout.CENTER);

    // --- Factures tab ---
    JPanel invoicesPane = new JPanel(new BorderLayout());
    JPanel ibar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    ibar.add(addInvoice);
    ibar.add(delInvoice);
    ibar.add(saveInvoice);
    ibar.add(reloadInvoice);
    ibar.add(pdfInvoice);
    ibar.add(pdfInvoiceDetail);
    ibar.add(mailInvoice);
    ibar.add(mailInvoiceInsertLines);
    ibar.add(csvInvoice);
    ibar.add(xlsInvoice);
    ibar.add(Box.createHorizontalStrut(12));
    ibar.add(searchInvoiceLbl);
    ibar.add(searchInvoice);
    invoicesTable.setModel(invoicesModel);
    invoicesTable.setFillsViewportHeight(true);
    invoicesTable.setRowHeight(24);
    invoicesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    invoicesTable.setDefaultRenderer(LocalDate.class, new LocalDateRenderer());
    invoiceSorter = new TableRowSorter<>(invoicesModel);
    invoicesTable.setRowSorter(invoiceSorter);
    invoicesPane.add(ibar, BorderLayout.NORTH);
    invoicesPane.add(new JScrollPane(invoicesTable), BorderLayout.CENTER);

    // Tabs
    tabs.addTab("Devis", quotesPane);
    tabs.addTab("Factures", invoicesPane);
    add(tabs, BorderLayout.CENTER);

    // Actions
    addQuote.addActionListener(e -> onAddQuote());
    delQuote.addActionListener(e -> onDeleteQuote());
    saveQuote.addActionListener(e -> onSaveQuote());
    reloadQuote.addActionListener(e -> reloadQuotes());
    pdfQuote.addActionListener(e -> onExportQuotesPdf());
    pdfQuoteDetail.addActionListener(e -> onExportQuoteDetailedPdf());
    mailQuote.addActionListener(e -> onEmailQuotesPdf());
    mailQuoteInsertLines.addActionListener(e -> onInsertQuoteLinesHtml());
    quoteToInvoice.addActionListener(e -> onGenerateInvoiceFromQuote());
    quoteToInvoicesMulti.addActionListener(e -> onGenerateInvoicesFromSelection());
    csvQuote.addActionListener(e -> onExportQuotesCsv());
    xlsQuote.addActionListener(e -> onExportQuotesExcel());
    addInvoice.addActionListener(e -> onAddInvoice());
    delInvoice.addActionListener(e -> onDeleteInvoice());
    saveInvoice.addActionListener(e -> onSaveInvoice());
    reloadInvoice.addActionListener(e -> reloadInvoices());
    pdfInvoice.addActionListener(e -> onExportInvoicesPdf());
    pdfInvoiceDetail.addActionListener(e -> onExportInvoiceDetailedPdf());
    mailInvoice.addActionListener(e -> onEmailInvoicesPdf());
    mailInvoiceInsertLines.addActionListener(e -> onInsertInvoiceLinesHtml());
    csvInvoice.addActionListener(e -> onExportInvoicesCsv());
    xlsInvoice.addActionListener(e -> onExportInvoicesExcel());

    DocumentListener quoteSearchListener = new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e){ filterQuotes(); }

      @Override public void removeUpdate(DocumentEvent e){ filterQuotes(); }

      @Override public void changedUpdate(DocumentEvent e){ filterQuotes(); }
    };
    searchQuote.getDocument().addDocumentListener(quoteSearchListener);

    DocumentListener invoiceSearchListener = new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e){ filterInvoices(); }

      @Override public void removeUpdate(DocumentEvent e){ filterInvoices(); }

      @Override public void changedUpdate(DocumentEvent e){ filterInvoices(); }
    };
    searchInvoice.getDocument().addDocumentListener(invoiceSearchListener);

    reload();
  }

  public void reload(){
    reloadQuotes();
    reloadInvoices();
  }

  private void reloadQuotes(){
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      quotesModel.setData(List.of());
      return;
    }
    try {
      List<QuoteV2> list = new ArrayList<>(sales.listQuotes());
      String agency = AgencyContext.agencyId();
      if (agency != null && !agency.isBlank()){
        list = list.stream().filter(q -> belongsToCurrentAgency(q.getAgencyId())).toList();
      }
      quotesModel.setData(list);
    } catch (Exception ex){
      Toasts.error(this, "Devis: " + ex.getMessage());
    }
  }

  private void onAddQuote(){
    QuoteV2 q = new QuoteV2();
    q.setDate(LocalDate.now());
    q.setClientName("Nouveau client");
    q.setReference("DRAFT-" + System.currentTimeMillis());
    q.setTotalHt(BigDecimal.ZERO);
    q.setTotalTtc(BigDecimal.ZERO);
    q.setSent(Boolean.FALSE);
    q.setAgencyId(AgencyContext.agencyId());
    quotesModel.add(q);
    int row = quotesModel.getRowCount() - 1;
    if (row >= 0){
      int viewRow = quotesTable.convertRowIndexToView(row);
      if (viewRow >= 0){
        quotesTable.setRowSelectionInterval(viewRow, viewRow);
        quotesTable.editCellAt(viewRow, 1);
      }
      quotesTable.requestFocusInWindow();
    }
  }

  private void onDeleteQuote(){
    int row = quotesTable.getSelectedRow();
    if (row < 0){
      return;
    }
    int modelRow = quotesTable.convertRowIndexToModel(row);
    QuoteV2 q = quotesModel.getAt(modelRow);
    if (q.getId() != null && !q.getId().isBlank()){
      try {
        ServiceLocator.sales().deleteQuote(q.getId());
      } catch (Exception ex){
        Toasts.error(this, "Suppression devis: " + ex.getMessage());
      }
    }
    quotesModel.remove(modelRow);
  }

  private void onSaveQuote(){
    stopEditing(quotesTable);
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      return;
    }
    List<QuoteV2> dirty = quotesModel.getData();
    int ok = 0;
    int ko = 0;
    for (QuoteV2 q : dirty){
      try {
        QuoteV2 saved = sales.saveQuote(q);
        merge(q, saved);
        ok++;
      } catch (Exception ex){
        ko++;
      }
    }
    if (ko == 0){
      Toasts.success(this, ok + " devis enregistrés");
    } else {
      Toasts.error(this, ok + " ok / " + ko + " erreurs");
    }
    reloadQuotes();
  }

  private void onExportQuotesPdf(){
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Exporter les devis (PDF)");
    chooser.setSelectedFile(new java.io.File("devis.pdf"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
      return;
    }
    try {
      java.io.File file = chooser.getSelectedFile();
      if (file == null){
        return;
      }
      PdfMini pdf = new PdfMini();
      pdf.addTitle("Liste des Devis");
      for (int i = 0; i < quotesTable.getRowCount(); i++){
        int modelRow = quotesTable.convertRowIndexToModel(i);
        QuoteV2 quote = quotesModel.getAt(modelRow);
        String line = String.format(Locale.ROOT,
            "• %s  |  %s  |  %s  |  HT: %s  TTC: %s%s",
            nz(quote.getReference()),
            nz(quote.getClientName()),
            formatDate(quote.getDate()),
            formatAmount(quote.getTotalHt()),
            formatAmount(quote.getTotalTtc()),
            Boolean.TRUE.equals(quote.getSent()) ? "  [envoyé]" : "");
        pdf.addParagraph(line.trim());
      }
      pdf.save(file);
      Toasts.success(this, "PDF exporté : " + file.getName());
    } catch (Exception ex){
      Toasts.error(this, "Export PDF devis : " + ex.getMessage());
    }
  }

  private void onExportQuoteDetailedPdf(){
    int row = quotesTable.getSelectedRow();
    if (row < 0){
      Toasts.info(this, "Sélectionnez un devis.");
      return;
    }
    row = quotesTable.convertRowIndexToModel(row);
    QuoteV2 quote = quotesModel.getAt(row);
    JFileChooser chooser = new JFileChooser();
    String name = "devis-" + nz(quote.getReference()).replaceAll("\\s+", "_") + ".pdf";
    chooser.setSelectedFile(new File(name));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
      return;
    }
    try {
      byte[] pdf = PdfTemplateEngine.renderQuote(quote, loadAgencyLogoBase64());
      Files.write(chooser.getSelectedFile().toPath(), pdf);
      Toasts.success(this, "PDF exporté : " + chooser.getSelectedFile().getName());
    } catch (Exception ex){
      Toasts.error(this, "PDF détaillé devis : " + ex.getMessage());
    }
  }

  private void onGenerateInvoiceFromQuote(){
    int row = quotesTable.getSelectedRow();
    if (row < 0){
      Toasts.info(this, "Sélectionnez un devis à convertir.");
      return;
    }
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      Toasts.error(this, "Service ventes indisponible.");
      return;
    }
    int modelRow = quotesTable.convertRowIndexToModel(row);
    QuoteV2 quote = quotesModel.getAt(modelRow);
    try {
      InvoiceV2 invoice = new InvoiceV2();
      invoice.setClientId(quote.getClientId());
      invoice.setClientName(quote.getClientName());
      invoice.setDate(LocalDate.now());
      invoice.setTotalHt(quote.getTotalHt());
      invoice.setTotalTtc(quote.getTotalTtc());
      invoice.setStatus("DRAFT");
      invoice.setAgencyId(AgencyContext.agencyId());
      invoice.setLines(quote.getLines());
      InvoiceV2 saved = sales.saveInvoice(invoice);
      String number = saved == null ? "—" : nz(saved.getNumber(), nz(saved.getId(), "—"));
      Toasts.success(this, "Facture générée n° " + number);
      reloadInvoices();
      tabs.setSelectedIndex(1);
    } catch (Exception ex){
      Toasts.error(this, "Échec génération facture : " + ex.getMessage());
    }
  }

  private void onGenerateInvoicesFromSelection(){
    int[] selected = quotesTable.getSelectedRows();
    if (selected == null || selected.length == 0){
      Toasts.info(this, "Sélectionnez un ou plusieurs devis.");
      return;
    }
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      Toasts.error(this, "Service ventes indisponible.");
      return;
    }
    int ok = 0;
    int ko = 0;
    for (int viewRow : selected){
      int modelRow = quotesTable.convertRowIndexToModel(viewRow);
      QuoteV2 quote = quotesModel.getAt(modelRow);
      try {
        InvoiceV2 invoice = new InvoiceV2();
        invoice.setClientId(quote.getClientId());
        invoice.setClientName(quote.getClientName());
        invoice.setDate(LocalDate.now());
        invoice.setTotalHt(quote.getTotalHt());
        invoice.setTotalTtc(quote.getTotalTtc());
        invoice.setStatus("DRAFT");
        invoice.setAgencyId(AgencyContext.agencyId());
        invoice.setLines(quote.getLines());
        sales.saveInvoice(invoice);
        ok++;
      } catch (Exception ex){
        ko++;
      }
    }
    if (ko == 0){
      Toasts.success(this, ok + " facture(s) générée(s).");
    } else {
      Toasts.error(this, ok + " ok / " + ko + " erreurs.");
    }
    reloadInvoices();
    tabs.setSelectedIndex(1);
  }

  private void reloadInvoices(){
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      invoicesModel.setData(List.of());
      return;
    }
    try {
      List<InvoiceV2> list = new ArrayList<>(sales.listInvoices());
      String agency = AgencyContext.agencyId();
      if (agency != null && !agency.isBlank()){
        list = list.stream().filter(f -> belongsToCurrentAgency(f.getAgencyId())).toList();
      }
      invoicesModel.setData(list);
    } catch (Exception ex){
      Toasts.error(this, "Factures: " + ex.getMessage());
    }
  }

  private void onAddInvoice(){
    InvoiceV2 f = new InvoiceV2();
    f.setDate(LocalDate.now());
    f.setClientName("Nouveau client");
    f.setStatus("DRAFT");
    f.setTotalHt(BigDecimal.ZERO);
    f.setTotalTtc(BigDecimal.ZERO);
    f.setAgencyId(AgencyContext.agencyId());
    invoicesModel.add(f);
    int row = invoicesModel.getRowCount() - 1;
    if (row >= 0){
      int viewRow = invoicesTable.convertRowIndexToView(row);
      if (viewRow >= 0){
        invoicesTable.setRowSelectionInterval(viewRow, viewRow);
        invoicesTable.editCellAt(viewRow, 1);
      }
      invoicesTable.requestFocusInWindow();
    }
  }

  private void onDeleteInvoice(){
    int row = invoicesTable.getSelectedRow();
    if (row < 0){
      return;
    }
    int modelRow = invoicesTable.convertRowIndexToModel(row);
    InvoiceV2 f = invoicesModel.getAt(modelRow);
    if (f.getId() != null && !f.getId().isBlank()){
      try {
        ServiceLocator.sales().deleteInvoice(f.getId());
      } catch (Exception ex){
        Toasts.error(this, "Suppression facture: " + ex.getMessage());
      }
    }
    invoicesModel.remove(modelRow);
  }

  private void onSaveInvoice(){
    stopEditing(invoicesTable);
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      return;
    }
    List<InvoiceV2> dirty = invoicesModel.getData();
    int ok = 0;
    int ko = 0;
    for (InvoiceV2 f : dirty){
      try {
        InvoiceV2 saved = sales.saveInvoice(f);
        merge(f, saved);
        ok++;
      } catch (Exception ex){
        ko++;
      }
    }
    if (ko == 0){
      Toasts.success(this, ok + " factures enregistrées");
    } else {
      Toasts.error(this, ok + " ok / " + ko + " erreurs");
    }
    reloadInvoices();
  }

  private void onExportInvoicesPdf(){
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Exporter les factures (PDF)");
    chooser.setSelectedFile(new java.io.File("factures.pdf"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
      return;
    }
    try {
      java.io.File file = chooser.getSelectedFile();
      if (file == null){
        return;
      }
      PdfMini pdf = new PdfMini();
      pdf.addTitle("Liste des Factures");
      for (int i = 0; i < invoicesTable.getRowCount(); i++){
        int modelRow = invoicesTable.convertRowIndexToModel(i);
        InvoiceV2 invoice = invoicesModel.getAt(modelRow);
        String line = String.format(Locale.ROOT,
            "• %s  |  %s  |  %s  |  HT: %s  TTC: %s  |  %s",
            nz(invoice.getNumber(), nz(invoice.getId(), "—")),
            nz(invoice.getClientName()),
            formatDate(invoice.getDate()),
            formatAmount(invoice.getTotalHt()),
            formatAmount(invoice.getTotalTtc()),
            nz(invoice.getStatus(), "—"));
        pdf.addParagraph(line.trim());
      }
      pdf.save(file);
      Toasts.success(this, "PDF exporté : " + file.getName());
    } catch (Exception ex){
      Toasts.error(this, "Export PDF factures : " + ex.getMessage());
    }
  }

  private void onExportInvoiceDetailedPdf(){
    int row = invoicesTable.getSelectedRow();
    if (row < 0){
      Toasts.info(this, "Sélectionnez une facture.");
      return;
    }
    row = invoicesTable.convertRowIndexToModel(row);
    InvoiceV2 invoice = invoicesModel.getAt(row);
    JFileChooser chooser = new JFileChooser();
    String name = "facture-" + nz(invoice.getNumber(), nz(invoice.getId(), "facture"))
        .replaceAll("\\s+", "_") + ".pdf";
    chooser.setSelectedFile(new File(name));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
      return;
    }
    try {
      byte[] pdf = PdfTemplateEngine.renderInvoice(invoice, loadAgencyLogoBase64());
      Files.write(chooser.getSelectedFile().toPath(), pdf);
      Toasts.success(this, "PDF exporté : " + chooser.getSelectedFile().getName());
    } catch (Exception ex){
      Toasts.error(this, "PDF détaillé facture : " + ex.getMessage());
    }
  }

  private void onExportQuotesCsv(){
    exportCsv("devis.csv", quotesTable, quotesModel);
  }

  private void onExportInvoicesCsv(){
    exportCsv("factures.csv", invoicesTable, invoicesModel);
  }

  private void exportCsv(String defaultName, JTable table, AbstractTableModel model){
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Exporter CSV");
    chooser.setSelectedFile(new File(defaultName));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
      return;
    }
    File file = chooser.getSelectedFile();
    if (file == null){
      return;
    }
    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)){
      for (int c = 0; c < model.getColumnCount(); c++){
        if (c > 0){
          writer.append(';');
        }
        writer.append(escapeCsv(model.getColumnName(c)));
      }
      writer.append('\n');
      int rowCount = table.getRowCount();
      for (int r = 0; r < rowCount; r++){
        int modelRow = table.convertRowIndexToModel(r);
        for (int c = 0; c < model.getColumnCount(); c++){
          if (c > 0){
            writer.append(';');
          }
          String value = exportValue(model.getValueAt(modelRow, c));
          writer.append(escapeCsv(value));
        }
        writer.append('\n');
      }
    } catch (IOException ex){
      Toasts.error(this, "Export CSV : " + ex.getMessage());
      return;
    }
    Toasts.success(this, "CSV exporté : " + file.getName());
  }

  private static String escapeCsv(String value){
    String text = value == null ? "" : value.replace("\"", "\"\"");
    return '"' + text + '"';
  }

  private void onExportQuotesExcel(){
    exportExcel("devis.xls", quotesTable, quotesModel, "Devis");
  }

  private void onExportInvoicesExcel(){
    exportExcel("factures.xls", invoicesTable, invoicesModel, "Factures");
  }

  private void exportExcel(String defaultName, JTable table, AbstractTableModel model, String sheetName){
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Exporter Excel");
    chooser.setSelectedFile(new File(defaultName));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
      return;
    }
    File file = chooser.getSelectedFile();
    if (file == null){
      return;
    }
    try {
      ExcelXml excel = new ExcelXml(sheetName);
      List<String> header = new ArrayList<>();
      for (int c = 0; c < model.getColumnCount(); c++){
        header.add(model.getColumnName(c));
      }
      excel.addRow(header);
      int rowCount = table.getRowCount();
      for (int r = 0; r < rowCount; r++){
        int modelRow = table.convertRowIndexToModel(r);
        List<String> row = new ArrayList<>();
        for (int c = 0; c < model.getColumnCount(); c++){
          row.add(exportValue(model.getValueAt(modelRow, c)));
        }
        excel.addRow(row);
      }
      excel.save(file);
      Toasts.success(this, "Excel exporté : " + file.getName());
    } catch (Exception ex){
      Toasts.error(this, "Export Excel : " + ex.getMessage());
    }
  }

  private String exportValue(Object value){
    if (value == null){
      return "";
    }
    if (value instanceof LocalDate localDate){
      return formatDate(localDate);
    }
    if (value instanceof BigDecimal amount){
      return formatAmount(amount);
    }
    if (value instanceof Boolean bool){
      return bool ? "true" : "false";
    }
    return value.toString();
  }

  private void onEmailQuotesPdf(){
    File pdf = quotesTable.getSelectedRow() >= 0 ? buildQuoteDetailedPdfTemp() : buildQuotesPdfTemp();
    if (pdf == null){
      return;
    }
    EmailPrompt.Result prompt = EmailPrompt.askWithTemplates(this, "Envoyer PDF Devis", buildQuoteEmailVars());
    if (prompt == null){
      pdf.delete();
      return;
    }
    MailService mail = ServiceLocator.mail();
    if (mail == null){
      Toasts.error(this, "Service email indisponible.");
      pdf.delete();
      return;
    }
    try {
      byte[] bytes = Files.readAllBytes(pdf.toPath());
      List<MailService.Attachment> attachments = new ArrayList<>();
      attachments.add(new MailService.Attachment(pdf.getName(), "application/pdf", bytes));
      mail.sendWithAttachments(prompt.to(), prompt.cc(), prompt.bcc(), prompt.subject(), prompt.body(), attachments);
      Toasts.success(this, "Email envoyé.");
    } catch (Exception ex){
      Toasts.error(this, "Envoi email : " + ex.getMessage());
    } finally {
      pdf.delete();
    }
  }

  private void onEmailInvoicesPdf(){
    File pdf = invoicesTable.getSelectedRow() >= 0 ? buildInvoiceDetailedPdfTemp() : buildInvoicesPdfTemp();
    if (pdf == null){
      return;
    }
    EmailPrompt.Result prompt = EmailPrompt.askWithTemplates(this, "Envoyer PDF Factures", buildInvoiceEmailVars());
    if (prompt == null){
      pdf.delete();
      return;
    }
    MailService mail = ServiceLocator.mail();
    if (mail == null){
      Toasts.error(this, "Service email indisponible.");
      pdf.delete();
      return;
    }
    try {
      byte[] bytes = Files.readAllBytes(pdf.toPath());
      List<MailService.Attachment> attachments = new ArrayList<>();
      attachments.add(new MailService.Attachment(pdf.getName(), "application/pdf", bytes));
      mail.sendWithAttachments(prompt.to(), prompt.cc(), prompt.bcc(), prompt.subject(), prompt.body(), attachments);
      Toasts.success(this, "Email envoyé.");
    } catch (Exception ex){
      Toasts.error(this, "Envoi email : " + ex.getMessage());
    } finally {
      pdf.delete();
    }
  }

  private Map<String, String> buildQuoteEmailVars(){
    Map<String, String> vars = new LinkedHashMap<>();
    populateAgencyVars(vars);
    vars.put("lines.tableHtml", "");
    int selectedRow = quotesTable.getSelectedRow();
    if (selectedRow >= 0){
      int modelRow = quotesTable.convertRowIndexToModel(selectedRow);
      QuoteV2 quote = quotesModel.getAt(modelRow);
      if (quote != null){
        vars.put("client.name", nz(quote.getClientName()));
        vars.put("quote.reference", nz(quote.getReference()));
        vars.put("quote.date", quote.getDate() == null ? "" : formatDate(quote.getDate()));
        vars.put("quote.totalHt", quote.getTotalHt() == null ? "" : formatAmount(quote.getTotalHt()));
        vars.put("quote.totalTtc", quote.getTotalTtc() == null ? "" : formatAmount(quote.getTotalTtc()));
        vars.put("lines.tableHtml", buildQuoteLinesHtml(quote));
      }
    }
    return vars;
  }

  private Map<String, String> buildInvoiceEmailVars(){
    Map<String, String> vars = new LinkedHashMap<>();
    populateAgencyVars(vars);
    vars.put("lines.tableHtml", "");
    int selectedRow = invoicesTable.getSelectedRow();
    if (selectedRow >= 0){
      int modelRow = invoicesTable.convertRowIndexToModel(selectedRow);
      InvoiceV2 invoice = invoicesModel.getAt(modelRow);
      if (invoice != null){
        vars.put("client.name", nz(invoice.getClientName()));
        vars.put("invoice.number", nz(invoice.getNumber(), nz(invoice.getId(), "")));
        vars.put("invoice.date", invoice.getDate() == null ? "" : formatDate(invoice.getDate()));
        vars.put("invoice.totalHt", invoice.getTotalHt() == null ? "" : formatAmount(invoice.getTotalHt()));
        vars.put("invoice.totalTtc", invoice.getTotalTtc() == null ? "" : formatAmount(invoice.getTotalTtc()));
        vars.put("invoice.status", nz(invoice.getStatus(), ""));
        vars.put("lines.tableHtml", buildInvoiceLinesHtml(invoice));
      }
    }
    return vars;
  }

  private void populateAgencyVars(Map<String, String> vars){
    vars.put("agency.name", nz(AgencyContext.agencyLabel()));
    vars.put("agency.addressHtml", "");
    vars.put("agency.vatRate", "");
    vars.put("agency.cgvHtml", "");
    vars.put("agency.emailCss", "");
    vars.put("agency.emailSignatureHtml", "");
    AgencyConfigGateway gateway = ServiceLocator.agencyConfig();
    if (gateway == null){
      return;
    }
    try {
      AgencyConfigGateway.AgencyConfig cfg = gateway.get();
      if (cfg != null){
        if (cfg.companyName() != null && !cfg.companyName().isBlank()){
          vars.put("agency.name", cfg.companyName());
        }
        vars.put("agency.addressHtml", nz(cfg.companyAddressHtml()));
        vars.put("agency.vatRate", cfg.vatRate() == null ? "" : cfg.vatRate().toString());
        vars.put("agency.cgvHtml", nz(cfg.cgvHtml()));
        vars.put("agency.emailCss", nz(cfg.emailCss()));
        vars.put("agency.emailSignatureHtml", nz(cfg.emailSignatureHtml()));
      }
    } catch (Exception ignore){
      // valeurs par défaut déjà positionnées
    }
  }

  private String buildQuoteLinesHtml(QuoteV2 quote){
    if (quote == null){
      return "";
    }
    try {
      return EmailTableBuilder.tableHtml(quote.getLines());
    } catch (Exception ex){
      return "";
    }
  }

  private String buildInvoiceLinesHtml(InvoiceV2 invoice){
    if (invoice == null){
      return "";
    }
    try {
      return EmailTableBuilder.tableHtml(invoice.getLines());
    } catch (Exception ex){
      return "";
    }
  }

  private void onInsertQuoteLinesHtml(){
    int row = quotesTable.getSelectedRow();
    if (row < 0){
      Toasts.info(this, "Sélectionnez un devis.");
      return;
    }
    row = quotesTable.convertRowIndexToModel(row);
    QuoteV2 quote = quotesModel.getAt(row);
    insertHtmlIntoEmailBody(buildQuoteLinesHtml(quote), "Insérer lignes (Devis)");
  }

  private void onInsertInvoiceLinesHtml(){
    int row = invoicesTable.getSelectedRow();
    if (row < 0){
      Toasts.info(this, "Sélectionnez une facture.");
      return;
    }
    row = invoicesTable.convertRowIndexToModel(row);
    InvoiceV2 invoice = invoicesModel.getAt(row);
    insertHtmlIntoEmailBody(buildInvoiceLinesHtml(invoice), "Insérer lignes (Facture)");
  }

  private void insertHtmlIntoEmailBody(String html, String title){
    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
    JTextArea textArea = new JTextArea(12, 60);
    textArea.setLineWrap(false);
    textArea.setText(html == null ? "" : html);
    textArea.setCaretPosition(0);
    dialog.add(new JScrollPane(textArea), BorderLayout.CENTER);
    JButton copy = new JButton("Copier dans le presse-papiers");
    copy.addActionListener(e -> {
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textArea.getText()), null);
      Toasts.success(SalesPanel.this, "Table HTML copiée. Collez-la dans votre message.");
      dialog.dispose();
    });
    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    south.add(copy);
    dialog.add(south, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
  }

  private File buildQuotesPdfTemp(){
    try {
      File file = File.createTempFile("devis-", ".pdf");
      file.deleteOnExit();
      PdfMini pdf = new PdfMini();
      pdf.addTitle("Liste des Devis");
      for (int i = 0; i < quotesTable.getRowCount(); i++){
        int modelRow = quotesTable.convertRowIndexToModel(i);
        QuoteV2 quote = quotesModel.getAt(modelRow);
        String line = String.format(Locale.ROOT,
            "• %s  |  %s  |  %s  |  HT: %s  TTC: %s%s",
            nz(quote.getReference()),
            nz(quote.getClientName()),
            formatDate(quote.getDate()),
            formatAmount(quote.getTotalHt()),
            formatAmount(quote.getTotalTtc()),
            Boolean.TRUE.equals(quote.getSent()) ? "  [envoyé]" : "");
        pdf.addParagraph(line.trim());
      }
      pdf.save(file);
      return file;
    } catch (Exception ex){
      Toasts.error(this, "Génération PDF : " + ex.getMessage());
      return null;
    }
  }

  private File buildInvoicesPdfTemp(){
    try {
      File file = File.createTempFile("factures-", ".pdf");
      file.deleteOnExit();
      PdfMini pdf = new PdfMini();
      pdf.addTitle("Liste des Factures");
      for (int i = 0; i < invoicesTable.getRowCount(); i++){
        int modelRow = invoicesTable.convertRowIndexToModel(i);
        InvoiceV2 invoice = invoicesModel.getAt(modelRow);
        String line = String.format(Locale.ROOT,
            "• %s  |  %s  |  %s  |  HT: %s  TTC: %s  |  %s",
            nz(invoice.getNumber(), nz(invoice.getId(), "—")),
            nz(invoice.getClientName()),
            formatDate(invoice.getDate()),
            formatAmount(invoice.getTotalHt()),
            formatAmount(invoice.getTotalTtc()),
            nz(invoice.getStatus(), "—"));
        pdf.addParagraph(line.trim());
      }
      pdf.save(file);
      return file;
    } catch (Exception ex){
      Toasts.error(this, "Génération PDF : " + ex.getMessage());
      return null;
    }
  }

  private File buildQuoteDetailedPdfTemp(){
    try {
      int row = quotesTable.getSelectedRow();
      if (row < 0){
        return null;
      }
      row = quotesTable.convertRowIndexToModel(row);
      QuoteV2 quote = quotesModel.getAt(row);
      byte[] pdf = PdfTemplateEngine.renderQuote(quote, loadAgencyLogoBase64());
      File file = File.createTempFile("devis-detail-", ".pdf");
      file.deleteOnExit();
      Files.write(file.toPath(), pdf);
      return file;
    } catch (Exception ex){
      Toasts.error(this, "Génération PDF : " + ex.getMessage());
      return null;
    }
  }

  private File buildInvoiceDetailedPdfTemp(){
    try {
      int row = invoicesTable.getSelectedRow();
      if (row < 0){
        return null;
      }
      row = invoicesTable.convertRowIndexToModel(row);
      InvoiceV2 invoice = invoicesModel.getAt(row);
      byte[] pdf = PdfTemplateEngine.renderInvoice(invoice, loadAgencyLogoBase64());
      File file = File.createTempFile("facture-detail-", ".pdf");
      file.deleteOnExit();
      Files.write(file.toPath(), pdf);
      return file;
    } catch (Exception ex){
      Toasts.error(this, "Génération PDF : " + ex.getMessage());
      return null;
    }
  }

  private String loadAgencyLogoBase64(){
    try (InputStream is = getClass().getResourceAsStream("/branding/logo.png")){
      if (is == null){
        return null;
      }
      byte[] bytes = is.readAllBytes();
      return Base64.getEncoder().encodeToString(bytes);
    } catch (IOException ex){
      return null;
    }
  }

  private void filterQuotes(){
    if (quoteSorter == null){
      return;
    }
    String text = searchQuote.getText();
    final String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    if (query.isEmpty()){
      quoteSorter.setRowFilter(null);
      return;
    }
    quoteSorter.setRowFilter(new RowFilter<>() {
      @Override public boolean include(Entry<? extends QuoteTableModel, ? extends Integer> entry){
        Integer identifier = entry.getIdentifier();
        if (identifier == null){
          return false;
        }
        int modelRow = identifier;
        if (modelRow < 0 || modelRow >= quotesModel.getRowCount()){
          return false;
        }
        QuoteV2 quote = quotesModel.getAt(modelRow);
        return containsIgnoreCase(quote.getReference(), query)
            || containsIgnoreCase(quote.getClientName(), query)
            || containsIgnoreCase(quote.getStatus(), query);
      }
    });
  }

  private void filterInvoices(){
    if (invoiceSorter == null){
      return;
    }
    String text = searchInvoice.getText();
    final String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    if (query.isEmpty()){
      invoiceSorter.setRowFilter(null);
      return;
    }
    invoiceSorter.setRowFilter(new RowFilter<>() {
      @Override public boolean include(Entry<? extends InvoiceTableModel, ? extends Integer> entry){
        Integer identifier = entry.getIdentifier();
        if (identifier == null){
          return false;
        }
        int modelRow = identifier;
        if (modelRow < 0 || modelRow >= invoicesModel.getRowCount()){
          return false;
        }
        InvoiceV2 invoice = invoicesModel.getAt(modelRow);
        return containsIgnoreCase(invoice.getNumber(), query)
            || containsIgnoreCase(invoice.getId(), query)
            || containsIgnoreCase(invoice.getClientName(), query)
            || containsIgnoreCase(invoice.getStatus(), query);
      }
    });
  }

  private boolean belongsToCurrentAgency(String agencyId){
    String current = AgencyContext.agencyId();
    if (current == null || current.isBlank()){
      return true;
    }
    return current.equals(agencyId);
  }

  private static boolean containsIgnoreCase(String value, String query){
    if (value == null || query == null || query.isEmpty()){
      return false;
    }
    return value.toLowerCase(Locale.ROOT).contains(query);
  }

  private static String formatDate(LocalDate date){
    return date == null ? "—" : DateTimeFormatter.ISO_LOCAL_DATE.format(date);
  }

  private static String formatAmount(BigDecimal amount){
    if (amount == null){
      return "0.00";
    }
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static String nz(String value){
    return value == null ? "" : value;
  }

  private static String nz(String value, String fallback){
    if (value == null || value.isBlank()){
      return fallback;
    }
    return value;
  }

  private static void stopEditing(JTable table){
    if (table.isEditing()){
      table.getCellEditor().stopCellEditing();
    }
  }

  private static void merge(QuoteV2 dst, QuoteV2 src){
    if (dst == null || src == null){
      return;
    }
    dst.setId(src.getId());
    dst.setReference(src.getReference());
    dst.setClientId(src.getClientId());
    dst.setClientName(src.getClientName());
    dst.setDate(src.getDate());
    dst.setStatus(src.getStatus());
    dst.setTotalHt(src.getTotalHt());
    dst.setTotalTtc(src.getTotalTtc());
    dst.setSent(src.getSent());
    dst.setAgencyId(src.getAgencyId());
    dst.setLines(src.getLines());
  }

  private static void merge(InvoiceV2 dst, InvoiceV2 src){
    if (dst == null || src == null){
      return;
    }
    dst.setId(src.getId());
    dst.setNumber(src.getNumber());
    dst.setClientId(src.getClientId());
    dst.setClientName(src.getClientName());
    dst.setDate(src.getDate());
    dst.setTotalHt(src.getTotalHt());
    dst.setTotalTtc(src.getTotalTtc());
    dst.setStatus(src.getStatus());
    dst.setAgencyId(src.getAgencyId());
    dst.setLines(src.getLines());
  }

  static class QuoteTableModel extends AbstractTableModel {
    private final String[] cols = {"ID", "Référence", "Client", "Date", "Total HT", "Total TTC", "Envoyé"};
    private final Class<?>[] types = {String.class, String.class, String.class, LocalDate.class, BigDecimal.class, BigDecimal.class, Boolean.class};
    private final List<QuoteV2> data = new ArrayList<>();

    public void setData(List<QuoteV2> list){
      data.clear();
      if (list != null){
        data.addAll(list);
      }
      fireTableDataChanged();
    }

    public List<QuoteV2> getData(){
      return data;
    }

    public void add(QuoteV2 quote){
      data.add(quote);
      int row = data.size() - 1;
      fireTableRowsInserted(row, row);
    }

    public void remove(int row){
      data.remove(row);
      fireTableRowsDeleted(row, row);
    }

    public QuoteV2 getAt(int row){
      return data.get(row);
    }

    @Override public int getRowCount(){
      return data.size();
    }

    @Override public int getColumnCount(){
      return cols.length;
    }

    @Override public String getColumnName(int column){
      return cols[column];
    }

    @Override public Class<?> getColumnClass(int columnIndex){
      return types[columnIndex];
    }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex){
      return columnIndex != 0;
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex){
      QuoteV2 q = data.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> q.getId();
        case 1 -> q.getReference();
        case 2 -> q.getClientName();
        case 3 -> q.getDate();
        case 4 -> q.getTotalHt();
        case 5 -> q.getTotalTtc();
        case 6 -> Boolean.TRUE.equals(q.getSent());
        default -> null;
      };
    }

    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex){
      QuoteV2 q = data.get(rowIndex);
      switch (columnIndex) {
        case 1 -> q.setReference(valueAsString(aValue));
        case 2 -> q.setClientName(valueAsString(aValue));
        case 3 -> q.setDate(asLocalDate(aValue));
        case 4 -> q.setTotalHt(asBigDecimal(aValue));
        case 5 -> q.setTotalTtc(asBigDecimal(aValue));
        case 6 -> q.setSent(Boolean.TRUE.equals(aValue));
        default -> {
        }
      }
      fireTableRowsUpdated(rowIndex, rowIndex);
    }
  }

  static class InvoiceTableModel extends AbstractTableModel {
    private final String[] cols = {"ID", "Numéro", "Client", "Date", "Total HT", "Total TTC", "Statut"};
    private final Class<?>[] types = {String.class, String.class, String.class, LocalDate.class, BigDecimal.class, BigDecimal.class, String.class};
    private final List<InvoiceV2> data = new ArrayList<>();

    public void setData(List<InvoiceV2> list){
      data.clear();
      if (list != null){
        data.addAll(list);
      }
      fireTableDataChanged();
    }

    public List<InvoiceV2> getData(){
      return data;
    }

    public void add(InvoiceV2 invoice){
      data.add(invoice);
      int row = data.size() - 1;
      fireTableRowsInserted(row, row);
    }

    public void remove(int row){
      data.remove(row);
      fireTableRowsDeleted(row, row);
    }

    public InvoiceV2 getAt(int row){
      return data.get(row);
    }

    @Override public int getRowCount(){
      return data.size();
    }

    @Override public int getColumnCount(){
      return cols.length;
    }

    @Override public String getColumnName(int column){
      return cols[column];
    }

    @Override public Class<?> getColumnClass(int columnIndex){
      return types[columnIndex];
    }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex){
      return columnIndex != 0;
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex){
      InvoiceV2 f = data.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> f.getId();
        case 1 -> f.getNumber();
        case 2 -> f.getClientName();
        case 3 -> f.getDate();
        case 4 -> f.getTotalHt();
        case 5 -> f.getTotalTtc();
        case 6 -> f.getStatus();
        default -> null;
      };
    }

    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex){
      InvoiceV2 f = data.get(rowIndex);
      switch (columnIndex) {
        case 1 -> f.setNumber(valueAsString(aValue));
        case 2 -> f.setClientName(valueAsString(aValue));
        case 3 -> f.setDate(asLocalDate(aValue));
        case 4 -> f.setTotalHt(asBigDecimal(aValue));
        case 5 -> f.setTotalTtc(asBigDecimal(aValue));
        case 6 -> f.setStatus(valueAsString(aValue));
        default -> {
        }
      }
      fireTableRowsUpdated(rowIndex, rowIndex);
    }
  }

  private static String valueAsString(Object value){
    return value == null ? null : value.toString();
  }

  private static BigDecimal asBigDecimal(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof BigDecimal bd){
      return bd;
    }
    if (value instanceof Number number){
      try {
        return new BigDecimal(number.toString());
      } catch (NumberFormatException ignore){
        return null;
      }
    }
    if (value instanceof String text){
      if (text.isBlank()){
        return null;
      }
      try {
        return new BigDecimal(text.replace(',', '.'));
      } catch (NumberFormatException ignore){
        return null;
      }
    }
    return null;
  }

  private static LocalDate asLocalDate(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof LocalDate localDate){
      return localDate;
    }
    if (value instanceof java.util.Date date){
      return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
    if (value instanceof Number number){
      return Instant.ofEpochMilli(number.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
    if (value instanceof String text){
      if (text.isBlank()){
        return null;
      }
      try {
        return LocalDate.parse(text);
      } catch (DateTimeParseException ignore){
        return null;
      }
    }
    return null;
  }

  private static class LocalDateRenderer extends DefaultTableCellRenderer {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override protected void setValue(Object value){
      if (value instanceof LocalDate localDate){
        setText(DF.format(localDate));
      } else {
        setText(value == null ? "" : value.toString());
      }
    }
  }
}
