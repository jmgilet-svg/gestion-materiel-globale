package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.model.InvoiceV2;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.service.SalesService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

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
  // Factures
  private final JTable invoicesTable = new JTable();
  private final InvoiceTableModel invoicesModel = new InvoiceTableModel();
  private final JButton addInvoice = new JButton("Nouvelle facture");
  private final JButton delInvoice = new JButton("Supprimer");
  private final JButton saveInvoice = new JButton("Enregistrer");
  private final JButton reloadInvoice = new JButton("Recharger");

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
    quotesTable.setModel(quotesModel);
    quotesTable.setFillsViewportHeight(true);
    quotesTable.setRowHeight(24);
    quotesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    quotesTable.setDefaultRenderer(LocalDate.class, new LocalDateRenderer());
    quotesPane.add(qbar, BorderLayout.NORTH);
    quotesPane.add(new JScrollPane(quotesTable), BorderLayout.CENTER);

    // --- Factures tab ---
    JPanel invoicesPane = new JPanel(new BorderLayout());
    JPanel ibar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    ibar.add(addInvoice);
    ibar.add(delInvoice);
    ibar.add(saveInvoice);
    ibar.add(reloadInvoice);
    invoicesTable.setModel(invoicesModel);
    invoicesTable.setFillsViewportHeight(true);
    invoicesTable.setRowHeight(24);
    invoicesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    invoicesTable.setDefaultRenderer(LocalDate.class, new LocalDateRenderer());
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
    addInvoice.addActionListener(e -> onAddInvoice());
    delInvoice.addActionListener(e -> onDeleteInvoice());
    saveInvoice.addActionListener(e -> onSaveInvoice());
    reloadInvoice.addActionListener(e -> reloadInvoices());

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
      quotesTable.setRowSelectionInterval(row, row);
      quotesTable.editCellAt(row, 1);
      quotesTable.requestFocusInWindow();
    }
  }

  private void onDeleteQuote(){
    int row = quotesTable.getSelectedRow();
    if (row < 0){
      return;
    }
    QuoteV2 q = quotesModel.getAt(row);
    if (q.getId() != null && !q.getId().isBlank()){
      try {
        ServiceLocator.sales().deleteQuote(q.getId());
      } catch (Exception ex){
        Toasts.error(this, "Suppression devis: " + ex.getMessage());
      }
    }
    quotesModel.remove(row);
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
      invoicesTable.setRowSelectionInterval(row, row);
      invoicesTable.editCellAt(row, 1);
      invoicesTable.requestFocusInWindow();
    }
  }

  private void onDeleteInvoice(){
    int row = invoicesTable.getSelectedRow();
    if (row < 0){
      return;
    }
    InvoiceV2 f = invoicesModel.getAt(row);
    if (f.getId() != null && !f.getId().isBlank()){
      try {
        ServiceLocator.sales().deleteInvoice(f.getId());
      } catch (Exception ex){
        Toasts.error(this, "Suppression facture: " + ex.getMessage());
      }
    }
    invoicesModel.remove(row);
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

  private boolean belongsToCurrentAgency(String agencyId){
    String current = AgencyContext.agencyId();
    if (current == null || current.isBlank()){
      return true;
    }
    return current.equals(agencyId);
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
