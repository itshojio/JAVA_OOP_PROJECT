package BTL_N11_HD;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Food & Drink POS - Java Swing thuần, không cần thư viện ngoài / MySQL. */
public class FoodDrinkPOS extends JFrame {
    private static final Color BG = new Color(9, 10, 12);
    private static final Color PANEL = new Color(16, 17, 19);
    private static final Color CARD = new Color(21, 22, 25);
    private static final Color BORDER = new Color(52, 54, 58);
    private static final Color TEXT = new Color(241, 242, 244);
    private static final Color MUTED = new Color(165, 167, 173);
    private static final Color BUTTON = new Color(42, 43, 47);
    private static final Color SUCCESS = new Color(101, 215, 148);
    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String DATA_FILE = "pos-data.dat";

    private Database data;
    private final Map<Integer, Integer> cart = new LinkedHashMap<Integer, Integer>();
    private final Map<String, StyledButton> navButtons = new LinkedHashMap<String, StyledButton>();
    private JPanel content, productGrid, cartList;
    private JLabel cartTotalLabel, cartTitle;
    private StyledButton payButton;
    private String currentPage = "Bán hàng", category = "Tất cả", searchText = "";

    static class Product implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String name, category, image;
        long price;
        Product(int id, String name, String category, long price, String image) {
            this.id = id; this.name = name; this.category = category;
            this.price = price; this.image = image;
        }
    }
    static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;
        String name; int quantity; long price;
        OrderItem(String name, int quantity, long price) {
            this.name = name; this.quantity = quantity; this.price = price;
        }
        long total() { return price * quantity; }
    }
    static class Sale implements Serializable {
        private static final long serialVersionUID = 1L;
        int id; LocalDateTime time; String method;
        List<OrderItem> items; long total, received;
        Sale(int id, LocalDateTime time, String method, List<OrderItem> items, long total, long received) {
            this.id = id; this.time = time; this.method = method;
            this.items = items; this.total = total; this.received = received;
        }
    }
    static class Database implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Product> products = new ArrayList<Product>();
        List<Sale> sales = new ArrayList<Sale>();
        int nextProductId = 1, nextSaleId = 1;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
                catch (Exception ignored) { }
                new FoodDrinkPOS().setVisible(true);
            }
        });
    }

    public FoodDrinkPOS() {
        super("Food & Drink POS - Java OOP");
        data = loadData();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(940, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(makeHeader(), BorderLayout.NORTH);
        content = new JPanel(new BorderLayout());
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(5, 18, 18, 18));
        add(content, BorderLayout.CENTER);
        showPage("Bán hàng");
    }

    private Database loadData() {
        if (Files.exists(Paths.get(DATA_FILE))) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
                Object obj = in.readObject();
                if (obj instanceof Database) return (Database) obj;
            } catch (Exception ex) {
                System.err.println("Khong doc duoc du lieu cu: " + ex.getMessage());
            }
        }
        Database db = new Database();
        db.products.add(new Product(db.nextProductId++, "Cà phê sữa", "Đồ uống", 30000, "assets/ca-phe-sua.jpg"));
        db.products.add(new Product(db.nextProductId++, "Trà sữa", "Đồ uống", 35000, "assets/tra-sua.jpg"));
        db.products.add(new Product(db.nextProductId++, "Bánh mì", "Đồ ăn", 25000, "assets/banh-mi.jpg"));
        db.products.add(new Product(db.nextProductId++, "Khoai tây chiên", "Đồ ăn", 25000, "assets/khoai-tay-chien.jpg"));
        return db;
    }

    private void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(data);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được dữ liệu: " + ex.getMessage(),
                    "Lỗi lưu dữ liệu", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel makeHeader() {
        JPanel header = plain(new BorderLayout(0, 15));
        header.setBorder(new EmptyBorder(20, 22, 12, 22));
        JPanel titleRow = plain(new BorderLayout());
        titleRow.add(label("☕  FOOD & DRINK POS", 20, true, TEXT), BorderLayout.WEST);
        titleRow.add(label("JAVA SWING  ·  BÁN HÀNG TẠI QUẦY", 12, false, MUTED), BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);
        JPanel tabs = plain(new FlowLayout(FlowLayout.LEFT, 10, 0));
        for (final String name : new String[]{"Bán hàng", "Quản lý món", "Đơn hàng", "Thống kê"}) {
            StyledButton b = new StyledButton(name, BUTTON);
            b.addActionListener(e -> showPage(name));
            tabs.add(b);
            navButtons.put(name, b);
        }
        header.add(tabs, BorderLayout.CENTER);
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER); sep.setBackground(BORDER);
        header.add(sep, BorderLayout.SOUTH);
        return header;
    }

    private void showPage(String page) {
        currentPage = page;
        for (Map.Entry<String, StyledButton> e : navButtons.entrySet()) {
            e.getValue().setBase(e.getKey().equals(page) ? new Color(70, 72, 78) : BUTTON);
        }
        content.removeAll();
        if (page.equals("Bán hàng")) content.add(makeSalePage(), BorderLayout.CENTER);
        else if (page.equals("Quản lý món")) content.add(makeManagePage(), BorderLayout.CENTER);
        else if (page.equals("Đơn hàng")) content.add(makeOrdersPage(), BorderLayout.CENTER);
        else content.add(makeStatisticsPage(), BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private JComponent makeSalePage() {
        JPanel left = plain(new BorderLayout(0, 13));
        left.setBorder(new EmptyBorder(8, 0, 0, 8));
        JPanel top = plain(new BorderLayout(0, 10));
        HintTextField search = new HintTextField("⌕  Tìm món ăn, nước uống...");
        search.setText(searchText);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() {
                searchText = search.getText().trim();
                refreshProductGrid();
            }
        });
        top.add(search, BorderLayout.NORTH);
        JPanel filters = plain(new FlowLayout(FlowLayout.LEFT, 7, 0));
        for (final String c : new String[]{"Tất cả", "Đồ uống", "Đồ ăn"}) {
            StyledButton button = new StyledButton(c, c.equals(category) ? new Color(80, 82, 87) : BUTTON);
            button.addActionListener(e -> { category = c; showPage("Bán hàng"); });
            filters.add(button);
        }
        top.add(filters, BorderLayout.SOUTH);
        left.add(top, BorderLayout.NORTH);

        productGrid = plain(new GridLayout(0, 2, 13, 13));
        JPanel gridWrapper = plain(new BorderLayout());
        gridWrapper.add(productGrid, BorderLayout.NORTH);
        JScrollPane productsScroll = scroll(gridWrapper);
        left.add(productsScroll, BorderLayout.CENTER);
        refreshProductGrid();

        RoundedPanel right = new RoundedPanel(PANEL, 20, BORDER);
        right.setLayout(new BorderLayout(0, 15));
        right.setBorder(new EmptyBorder(20, 18, 18, 18));
        cartTitle = label("Đơn hàng #" + numberId(data.nextSaleId), 21, true, TEXT);
        JPanel rightTop = plain(new BorderLayout());
        rightTop.add(cartTitle, BorderLayout.CENTER);
        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER); separator.setBackground(BORDER);
        rightTop.add(separator, BorderLayout.SOUTH);
        rightTop.setBorder(new EmptyBorder(0, 0, 13, 0));
        right.add(rightTop, BorderLayout.NORTH);
        cartList = plain();
        cartList.setLayout(new BoxLayout(cartList, BoxLayout.Y_AXIS));
        right.add(scroll(cartList), BorderLayout.CENTER);
        JPanel footer = plain();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        JPanel totalRow = plain(new BorderLayout());
        totalRow.add(label("Tổng cộng", 18, true, TEXT), BorderLayout.WEST);
        cartTotalLabel = label("0đ", 24, true, TEXT);
        totalRow.add(cartTotalLabel, BorderLayout.EAST);
        totalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(totalRow);
        footer.add(Box.createVerticalStrut(14));
        payButton = new StyledButton("Thanh toán", new Color(65, 67, 73));
        payButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        payButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        payButton.setPreferredSize(new Dimension(400, 48));
        payButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        payButton.addActionListener(e -> openPayment());
        footer.add(payButton);
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));
        right.add(footer, BorderLayout.SOUTH);
        refreshCart();

        JPanel columns = plain(new GridLayout(1, 2, 14, 0));
        columns.add(left);
        columns.add(right);
        return columns;
    }

    private void refreshProductGrid() {
        if (productGrid == null) return;
        productGrid.removeAll();
        String q = searchText.toLowerCase(Locale.ROOT);
        int found = 0;
        for (Product p : data.products) {
            if (!category.equals("Tất cả") && !category.equals(p.category)) continue;
            if (!p.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            productGrid.add(makeProductCard(p));
            found++;
        }
        if (found == 0) {
            productGrid.add(label("Không tìm thấy món phù hợp.", 15, false, MUTED));
        }
        productGrid.revalidate();
        productGrid.repaint();
    }

    private JPanel makeProductCard(final Product p) {
        RoundedPanel card = new RoundedPanel(CARD, 17, BORDER);
        card.setLayout(new BorderLayout(0, 5));
        card.setBorder(new EmptyBorder(6, 6, 8, 6));
        card.setPreferredSize(new Dimension(240, 251));
        ImagePanel image = new ImagePanel(p.image);
        image.setPreferredSize(new Dimension(230, 185));
        card.add(image, BorderLayout.CENTER);
        JPanel bottom = plain(new BorderLayout(5, 0));
        JPanel texts = plain();
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(label(p.name, 14, true, TEXT));
        texts.add(Box.createVerticalStrut(2));
        texts.add(label(money(p.price), 13, false, MUTED));
        bottom.add(texts, BorderLayout.CENTER);
        StyledButton add = new StyledButton("+", new Color(54, 56, 62));
        add.setFont(new Font("SansSerif", Font.BOLD, 19));
        add.setPreferredSize(new Dimension(39, 39));
        add.setBorder(new EmptyBorder(0, 0, 0, 0));
        add.addActionListener(e -> addToCart(p.id));
        bottom.add(add, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);
        MouseAdapter click = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { addToCart(p.id); }
            public void mouseEntered(MouseEvent e) { card.setOutline(new Color(115, 117, 124)); }
            public void mouseExited(MouseEvent e) { card.setOutline(BORDER); }
        };
        card.addMouseListener(click);
        image.addMouseListener(click);
        texts.addMouseListener(click);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        image.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return card;
    }

    private void addToCart(int productId) {
        cart.put(productId, cart.containsKey(productId) ? cart.get(productId) + 1 : 1);
        refreshCart();
    }

    private void modifyQuantity(int productId, int change) {
        int quantity = cart.containsKey(productId) ? cart.get(productId) + change : 0;
        if (quantity <= 0) cart.remove(productId);
        else cart.put(productId, quantity);
        refreshCart();
    }

    private Product getProduct(int id) {
        for (Product p : data.products) if (p.id == id) return p;
        return null;
    }

    private long cartTotal() {
        long total = 0;
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            Product p = getProduct(entry.getKey());
            if (p != null) total += p.price * entry.getValue();
        }
        return total;
    }

    private void refreshCart() {
        if (cartList == null || cartTotalLabel == null) return;
        cartList.removeAll();
        if (cart.isEmpty()) {
            JLabel empty = label("Chưa có món nào. Hãy chọn món ở bên trái.", 14, false, MUTED);
            empty.setBorder(new EmptyBorder(16, 0, 0, 0));
            cartList.add(empty);
        } else {
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                final Product p = getProduct(entry.getKey());
                if (p == null) continue;
                int qty = entry.getValue();
                JPanel row = plain(new BorderLayout(10, 0));
                row.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER),
                        new EmptyBorder(13, 0, 13, 0)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
                JPanel detail = plain();
                detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
                detail.add(label(p.name, 14, true, TEXT));
                detail.add(Box.createVerticalStrut(4));
                detail.add(label(qty + " × " + money(p.price), 12, false, MUTED));
                row.add(detail, BorderLayout.CENTER);
                JPanel actions = plain();
                actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
                JLabel amount = label(money(p.price * qty), 14, false, TEXT);
                amount.setAlignmentX(Component.RIGHT_ALIGNMENT);
                actions.add(amount);
                actions.add(Box.createVerticalStrut(6));
                JPanel count = plain(new FlowLayout(FlowLayout.RIGHT, 7, 0));
                StyledButton minus = new StyledButton("−", BUTTON);
                StyledButton plus = new StyledButton("+", BUTTON);
                for (StyledButton b : new StyledButton[]{minus, plus}) {
                    b.setFont(new Font("SansSerif", Font.BOLD, 14));
                    b.setPreferredSize(new Dimension(28, 27));
                    b.setBorder(new EmptyBorder(0, 0, 0, 0));
                }
                minus.addActionListener(e -> modifyQuantity(p.id, -1));
                plus.addActionListener(e -> modifyQuantity(p.id, 1));
                count.add(minus);
                count.add(label(String.valueOf(qty), 13, true, TEXT));
                count.add(plus);
                count.setAlignmentX(Component.RIGHT_ALIGNMENT);
                actions.add(count);
                row.add(actions, BorderLayout.EAST);
                cartList.add(row);
            }
        }
        cartTotalLabel.setText(money(cartTotal()));
        if (cartTitle != null) cartTitle.setText("Đơn hàng #" + numberId(data.nextSaleId));
        if (payButton != null) payButton.setEnabled(!cart.isEmpty());
        cartList.revalidate();
        cartList.repaint();
    }

    private void openPayment() {
        if (cart.isEmpty()) return;
        final long total = cartTotal();
        JDialog dialog = makeDialog("Thanh toán đơn #" + numberId(data.nextSaleId), 450, 370);
        JPanel box = formRoot();
        box.add(label("Xác nhận thanh toán", 22, true, TEXT));
        box.add(Box.createVerticalStrut(12));
        box.add(label("Tổng tiền: " + money(total), 18, true, TEXT));
        box.add(Box.createVerticalStrut(18));
        box.add(label("Phương thức thanh toán", 13, false, MUTED));
        JComboBox<String> method = combo(new String[]{"Tiền mặt", "Chuyển khoản (mô phỏng)"});
        box.add(method);
        box.add(Box.createVerticalStrut(11));
        box.add(label("Tiền khách đưa (đối với tiền mặt)", 13, false, MUTED));
        JTextField received = input(String.valueOf(total));
        box.add(received);
        box.add(Box.createVerticalStrut(10));
        JLabel changeLabel = label("Tiền thừa: 0đ", 14, true, SUCCESS);
        box.add(changeLabel);
        Runnable computeChange = () -> {
            boolean cash = method.getSelectedIndex() == 0;
            received.setEnabled(cash);
            if (!cash) { changeLabel.setText("Thanh toán chuyển khoản (mô phỏng)"); return; }
            try {
                long amount = parsePrice(received.getText());
                changeLabel.setText(amount >= total ? "Tiền thừa: " + money(amount - total) : "Chưa đủ: " + money(total - amount));
            } catch (Exception ex) { changeLabel.setText("Vui lòng nhập số tiền hợp lệ"); }
        };
        method.addActionListener(e -> computeChange.run());
        received.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { computeChange.run(); }
            public void removeUpdate(DocumentEvent e) { computeChange.run(); }
            public void changedUpdate(DocumentEvent e) { computeChange.run(); }
        });
        box.add(Box.createVerticalGlue());
        StyledButton confirm = new StyledButton("Xác nhận thanh toán", new Color(72, 90, 78));
        confirm.setPreferredSize(new Dimension(390, 45));
        confirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        confirm.addActionListener(e -> {
            boolean cash = method.getSelectedIndex() == 0;
            long amount = total;
            if (cash) {
                try { amount = parsePrice(received.getText()); }
                catch (Exception ex) { message("Tiền khách đưa không hợp lệ."); return; }
                if (amount < total) { message("Tiền khách đưa chưa đủ."); return; }
            }
            List<OrderItem> items = new ArrayList<OrderItem>();
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                Product p = getProduct(entry.getKey());
                if (p != null) items.add(new OrderItem(p.name, entry.getValue(), p.price));
            }
            Sale sale = new Sale(data.nextSaleId++, LocalDateTime.now(),
                    cash ? "Tiền mặt" : "Chuyển khoản (mô phỏng)", items, total, amount);
            data.sales.add(sale);
            saveData();
            cart.clear();
            dialog.dispose();
            showPage("Bán hàng");
            openInvoice(sale);
        });
        box.add(Box.createVerticalStrut(12));
        box.add(confirm);
        dialog.setContentPane(box);
        dialog.setVisible(true);
    }

    private void openInvoice(Sale sale) {
        JDialog dialog = makeDialog("Hóa đơn #" + numberId(sale.id), 530, 550);
        JPanel root = plain(new BorderLayout(0, 15));
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel success = label("✓ Thanh toán thành công   ·   Hóa đơn #" + numberId(sale.id), 18, true, SUCCESS);
        root.add(success, BorderLayout.NORTH);
        JTextArea output = new JTextArea(invoiceText(sale));
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 13));
        output.setBackground(CARD); output.setForeground(TEXT);
        output.setBorder(new EmptyBorder(16, 18, 16, 18));
        output.setCaretPosition(0);
        root.add(scroll(output), BorderLayout.CENTER);
        JPanel buttons = plain(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        StyledButton save = new StyledButton("Lưu hóa đơn .txt", new Color(65, 70, 84));
        save.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("HoaDon_" + numberId(sale.id) + ".txt"));
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.write(chooser.getSelectedFile().toPath(), invoiceText(sale).getBytes(StandardCharsets.UTF_8));
                    message("Đã lưu hóa đơn thành công.");
                } catch (IOException ex) { message("Không thể lưu file: " + ex.getMessage()); }
            }
        });
        StyledButton close = new StyledButton("Đóng", BUTTON);
        close.addActionListener(e -> dialog.dispose());
        buttons.add(save); buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private String invoiceText(Sale s) {
        StringBuilder sb = new StringBuilder();
        sb.append("          FOOD & DRINK POS\n");
        sb.append("        HÓA ĐƠN BÁN HÀNG\n");
        sb.append("------------------------------------\n");
        sb.append("Mã đơn: #").append(numberId(s.id)).append("\n");
        sb.append("Thời gian: ").append(s.time.format(TIME_FORMAT)).append("\n");
        sb.append("------------------------------------\n");
        for (OrderItem item : s.items) {
            sb.append(item.name).append("\n");
            sb.append("  ").append(item.quantity).append(" x ").append(money(item.price));
            sb.append("   = ").append(money(item.total())).append("\n");
        }
        sb.append("------------------------------------\n");
        sb.append("TỔNG CỘNG: ").append(money(s.total)).append("\n");
        sb.append("Thanh toán: ").append(s.method).append("\n");
        if (s.method.equals("Tiền mặt")) {
            sb.append("Khách đưa: ").append(money(s.received)).append("\n");
            sb.append("Tiền thừa: ").append(money(s.received - s.total)).append("\n");
        }
        sb.append("------------------------------------\n");
        sb.append("       Cảm ơn quý khách!\n");
        return sb.toString();
    }

    private JComponent makeManagePage() {
        JPanel root = plain(new BorderLayout(0, 15));
        root.setBorder(new EmptyBorder(12, 8, 4, 8));
        JPanel title = plain(new BorderLayout());
        title.add(pageHeading("Quản lý món", "Thêm, sửa, xóa sản phẩm trong thực đơn"), BorderLayout.WEST);
        StyledButton add = new StyledButton("+ Thêm món mới", new Color(64, 74, 83));
        add.addActionListener(e -> editProduct(null));
        title.add(add, BorderLayout.EAST);
        root.add(title, BorderLayout.NORTH);
        JPanel rows = plain();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (Product p : data.products) {
            RoundedPanel row = new RoundedPanel(CARD, 14, BORDER);
            row.setLayout(new BorderLayout(12, 0));
            row.setBorder(new EmptyBorder(10, 12, 10, 12));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 102));
            ImagePanel img = new ImagePanel(p.image);
            img.setPreferredSize(new Dimension(102, 78));
            row.add(img, BorderLayout.WEST);
            JPanel detail = plain(); detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
            detail.add(label(p.name, 17, true, TEXT));
            detail.add(Box.createVerticalStrut(6));
            detail.add(label(p.category + "    ·    " + money(p.price), 14, false, MUTED));
            row.add(detail, BorderLayout.CENTER);
            JPanel actions = plain(new FlowLayout(FlowLayout.RIGHT, 8, 12));
            StyledButton edit = new StyledButton("Sửa", BUTTON);
            StyledButton delete = new StyledButton("Xóa", new Color(93, 54, 56));
            edit.addActionListener(e -> editProduct(p));
            delete.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(this, "Xóa món '" + p.name + "'?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    data.products.remove(p); cart.remove(p.id); saveData(); showPage("Quản lý món");
                }
            });
            actions.add(edit); actions.add(delete);
            row.add(actions, BorderLayout.EAST);
            JPanel spacing = plain(new BorderLayout());
            spacing.setBorder(new EmptyBorder(0, 0, 11, 0));
            spacing.add(row);
            spacing.setMaximumSize(new Dimension(Integer.MAX_VALUE, 113));
            rows.add(spacing);
        }
        JPanel wrap = plain(new BorderLayout()); wrap.add(rows, BorderLayout.NORTH);
        root.add(scroll(wrap), BorderLayout.CENTER);
        return root;
    }

    private void editProduct(Product existing) {
        JDialog dialog = makeDialog(existing == null ? "Thêm món mới" : "Chỉnh sửa món", 470, 460);
        JPanel form = formRoot();
        form.add(label(existing == null ? "Thêm sản phẩm" : "Chỉnh sửa sản phẩm", 21, true, TEXT));
        form.add(Box.createVerticalStrut(14));
        form.add(label("Tên sản phẩm", 13, false, MUTED));
        JTextField name = input(existing == null ? "" : existing.name); form.add(name);
        form.add(Box.createVerticalStrut(12));
        form.add(label("Danh mục", 13, false, MUTED));
        JComboBox<String> cat = combo(new String[]{"Đồ uống", "Đồ ăn"});
        if (existing != null) cat.setSelectedItem(existing.category);
        form.add(cat);
        form.add(Box.createVerticalStrut(12));
        form.add(label("Giá bán (VNĐ)", 13, false, MUTED));
        JTextField price = input(existing == null ? "" : String.valueOf(existing.price)); form.add(price);
        form.add(Box.createVerticalStrut(12));
        form.add(label("Hình ảnh sản phẩm (tùy chọn)", 13, false, MUTED));
        final String[] picture = { existing == null ? "" : existing.image };
        StyledButton choose = new StyledButton("Chọn ảnh từ máy tính...", BUTTON);
        choose.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File source = chooser.getSelectedFile();
                try {
                    BufferedImage test = ImageIO.read(source);
                    if (test == null) { message("Định dạng ảnh không hỗ trợ."); return; }
                    Path folder = Paths.get("assets"); Files.createDirectories(folder);
                    String ext = source.getName().toLowerCase(Locale.ROOT).endsWith(".png") ? ".png" : ".jpg";
                    Path destination = folder.resolve("custom-" + System.currentTimeMillis() + ext);
                    // Lưu PNG/JPEG để thư mục dự án có thể mang sang máy khác.
                    if (ext.equals(".png")) ImageIO.write(test, "png", destination.toFile());
                    else ImageIO.write(test, "jpg", destination.toFile());
                    picture[0] = destination.toString();
                    choose.setText("Đã chọn: " + source.getName());
                } catch (IOException ex) { message("Không đọc được ảnh: " + ex.getMessage()); }
            }
        });
        form.add(choose);
        form.add(Box.createVerticalGlue());
        StyledButton save = new StyledButton("Lưu sản phẩm", new Color(65, 82, 71));
        save.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        save.addActionListener(e -> {
            String n = name.getText().trim();
            long value;
            try { value = parsePrice(price.getText()); }
            catch (Exception ex) { message("Giá phải là số nguyên dương."); return; }
            if (n.isEmpty() || value <= 0) { message("Vui lòng nhập tên món và giá > 0."); return; }
            if (existing == null) {
                data.products.add(new Product(data.nextProductId++, n, (String) cat.getSelectedItem(), value, picture[0]));
            } else {
                existing.name = n; existing.category = (String) cat.getSelectedItem();
                existing.price = value; existing.image = picture[0];
            }
            saveData(); dialog.dispose(); showPage("Quản lý món");
        });
        form.add(save);
        dialog.setContentPane(form);
        dialog.setVisible(true);
    }

    private JComponent makeOrdersPage() {
        JPanel root = plain(new BorderLayout(0, 15));
        root.setBorder(new EmptyBorder(12, 8, 4, 8));
        root.add(pageHeading("Lịch sử đơn hàng", "Các đơn đã thanh toán được lưu tự động"), BorderLayout.NORTH);
        JPanel rows = plain();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        if (data.sales.isEmpty()) rows.add(label("Chưa có đơn hàng nào được thanh toán.", 15, false, MUTED));
        for (int i = data.sales.size() - 1; i >= 0; i--) {
            Sale sale = data.sales.get(i);
            RoundedPanel row = new RoundedPanel(CARD, 14, BORDER);
            row.setLayout(new BorderLayout(15, 0));
            row.setBorder(new EmptyBorder(18, 20, 18, 20));
            JPanel detail = plain(); detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
            detail.add(label("Đơn #" + numberId(sale.id) + "    ·    " + money(sale.total), 17, true, TEXT));
            detail.add(Box.createVerticalStrut(6));
            detail.add(label(sale.time.format(TIME_FORMAT) + "    ·    " + sale.method, 13, false, MUTED));
            row.add(detail, BorderLayout.CENTER);
            StyledButton view = new StyledButton("Xem hóa đơn", BUTTON);
            view.addActionListener(e -> openInvoice(sale));
            row.add(view, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            JPanel gap = plain(new BorderLayout()); gap.add(row); gap.setBorder(new EmptyBorder(0, 0, 10, 0));
            gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); rows.add(gap);
        }
        JPanel wrap = plain(new BorderLayout()); wrap.add(rows, BorderLayout.NORTH);
        root.add(scroll(wrap), BorderLayout.CENTER);
        return root;
    }

    private JComponent makeStatisticsPage() {
        JPanel root = plain(new BorderLayout(0, 20));
        root.setBorder(new EmptyBorder(12, 8, 4, 8));
        root.add(pageHeading("Thống kê bán hàng", "Tổng hợp từ các đơn hàng đã thanh toán"), BorderLayout.NORTH);
        long revenue = 0; int count = 0;
        Map<String, Integer> quantities = new HashMap<String, Integer>();
        Map<String, Long> revenues = new HashMap<String, Long>();
        for (Sale s : data.sales) {
            revenue += s.total;
            for (OrderItem item : s.items) {
                count += item.quantity;
                quantities.put(item.name, quantities.containsKey(item.name) ? quantities.get(item.name) + item.quantity : item.quantity);
                revenues.put(item.name, revenues.containsKey(item.name) ? revenues.get(item.name) + item.total() : item.total());
            }
        }
        String best = "Chưa có";
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            if (entry.getValue() > bestCount) { best = entry.getKey(); bestCount = entry.getValue(); }
        }
        JPanel body = plain(new BorderLayout(0, 20));
        JPanel metrics = plain(new GridLayout(1, 4, 12, 0));
        metrics.add(metric("Doanh thu", money(revenue)));
        metrics.add(metric("Đơn đã bán", String.valueOf(data.sales.size())));
        metrics.add(metric("Số lượng món", String.valueOf(count)));
        metrics.add(metric("Món bán chạy", best));
        body.add(metrics, BorderLayout.NORTH);
        RoundedPanel summary = new RoundedPanel(PANEL, 16, BORDER);
        summary.setLayout(new BorderLayout(0, 10));
        summary.setBorder(new EmptyBorder(18, 20, 18, 20));
        summary.add(label("Doanh thu theo món", 19, true, TEXT), BorderLayout.NORTH);
        String[] cols = {"Tên món", "Số lượng bán", "Doanh thu"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        List<String> names = new ArrayList<String>(quantities.keySet());
        Collections.sort(names, (a, b) -> quantities.get(b).compareTo(quantities.get(a)));
        for (String name : names) model.addRow(new Object[]{name, quantities.get(name), money(revenues.get(name))});
        JTable table = new JTable(model);
        table.setRowHeight(42); table.setBackground(PANEL); table.setForeground(TEXT);
        table.setFont(FONT); table.setGridColor(BORDER); table.setSelectionBackground(new Color(67, 69, 74));
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(BUTTON);
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        summary.add(scroll(table), BorderLayout.CENTER);
        body.add(summary, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel metric(String caption, String value) {
        RoundedPanel p = new RoundedPanel(CARD, 15, BORDER);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 14, 20, 14));
        p.add(label(caption, 13, false, MUTED));
        p.add(Box.createVerticalStrut(11));
        p.add(label(value, 19, true, TEXT));
        return p;
    }

    private JPanel pageHeading(String title, String subtitle) {
        JPanel p = plain(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label(title, 23, true, TEXT));
        p.add(Box.createVerticalStrut(5));
        p.add(label(subtitle, 13, false, MUTED));
        return p;
    }

    private static String money(long amount) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }
    private static String numberId(int id) { return String.format("%03d", id); }
    private static long parsePrice(String text) {
        String stripped = text.trim().replaceAll("[.,\\sđ₫]", "");
        if (!stripped.matches("[0-9]+")) throw new NumberFormatException();
        return Long.parseLong(stripped);
    }
    private void message(String text) { JOptionPane.showMessageDialog(this, text); }
    private static JPanel plain() { return plain(new FlowLayout(FlowLayout.LEFT, 0, 0)); }
    private static JPanel plain(LayoutManager layout) { JPanel p = new JPanel(layout); p.setOpaque(false); return p; }
    private static JLabel label(String s, int size, boolean bold, Color color) {
        JLabel l = new JLabel(s);
        l.setForeground(color);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        return l;
    }
    private static JScrollPane scroll(Component c) {
        JScrollPane sc = new JScrollPane(c);
        sc.setBorder(null); sc.setOpaque(false); sc.getViewport().setOpaque(false);
        sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sc.getVerticalScrollBar().setUnitIncrement(20);
        sc.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        sc.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected JButton createDecreaseButton(int orientation) { return emptyArrowButton(); }
            protected JButton createIncreaseButton(int orientation) { return emptyArrowButton(); }
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(BG); g.fillRect(r.x, r.y, r.width, r.height);
            }
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                g.setColor(new Color(83, 86, 92));
                g.fillRoundRect(r.x + 2, r.y, Math.max(3, r.width - 4), r.height, 8, 8);
            }
        });
        return sc;
    }
    private static JButton emptyArrowButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }
    private static JTextField input(String text) {
        JTextField f = new JTextField(text);
        f.setFont(FONT); f.setForeground(TEXT); f.setCaretColor(TEXT); f.setBackground(BUTTON);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(10, 12, 10, 12)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        return f;
    }
    private static JComboBox<String> combo(String[] values) {
        JComboBox<String> c = new JComboBox<String>(values);
        c.setFont(FONT); c.setBackground(BUTTON); c.setForeground(TEXT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        c.setPreferredSize(new Dimension(380, 43));
        return c;
    }
    private JPanel formRoot() {
        JPanel p = plain();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 23, 22, 23));
        p.setBackground(PANEL); p.setOpaque(true);
        return p;
    }
    private JDialog makeDialog(String title, int width, int height) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(width, height); d.setMinimumSize(new Dimension(width, height));
        d.setLocationRelativeTo(this); d.getContentPane().setBackground(PANEL);
        return d;
    }

    /** Nền bo góc + viền mảnh. */
    static class RoundedPanel extends JPanel {
        private Color fill, outline; private int radius;
        RoundedPanel(Color fill, int radius, Color outline) {
            this.fill = fill; this.radius = radius; this.outline = outline; setOpaque(false);
        }
        void setOutline(Color color) { outline = color; repaint(); }
        protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.setColor(outline);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.dispose(); super.paintComponent(gr);
        }
    }

    /** Nút bo góc, có trạng thái hover và disabled. */
    static class StyledButton extends JButton {
        private Color base; private boolean hovered;
        StyledButton(String title, Color base) {
            super(title); this.base = base;
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setForeground(TEXT); setContentAreaFilled(false); setFocusPainted(false);
            setBorder(new EmptyBorder(10, 15, 10, 15));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }
        void setBase(Color newBase) { base = newBase; repaint(); }
        protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(35, 36, 39) : hovered ? base.brighter() : base;
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
            super.paintComponent(gr);
        }
    }

    /** JTextField hiển thị gợi ý khi chưa nhập. */
    static class HintTextField extends JTextField {
        private final String hint;
        HintTextField(String hint) {
            this.hint = hint;
            setFont(FONT); setForeground(TEXT); setCaretColor(TEXT);
            setBackground(PANEL);
            setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(11, 15, 11, 15)));
        }
        protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            if (!getText().isEmpty()) return;
            Graphics2D g = (Graphics2D) gr.create();
            g.setColor(MUTED); g.setFont(getFont());
            Insets in = getInsets();
            g.drawString(hint, in.left, (getHeight() + g.getFontMetrics().getAscent()) / 2 - 2);
            g.dispose();
        }
    }

    /** Ảnh được crop vừa khung, góc bo; vẫn chạy khi file ảnh bị thiếu. */
    static class ImagePanel extends JPanel {
        private BufferedImage image;
        ImagePanel(String path) {
            setOpaque(false);
            if (path != null && !path.isEmpty()) {
                try { image = ImageIO.read(new File(path)); }
                catch (IOException ignored) { }
            }
        }
        protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) { g.dispose(); return; }
            g.setClip(new RoundRectangle2D.Double(0, 0, w, h, 12, 12));
            g.setColor(new Color(48, 50, 54)); g.fillRect(0, 0, w, h);
            if (image != null) {
                double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                int dw = (int) Math.ceil(image.getWidth() * scale);
                int dh = (int) Math.ceil(image.getHeight() * scale);
                g.drawImage(image, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                g.setColor(MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.drawString("Chưa có ảnh", Math.max(10, w / 2 - 42), h / 2);
            }
            g.dispose();
        }
    }
}
