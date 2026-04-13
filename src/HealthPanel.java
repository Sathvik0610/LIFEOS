import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;

public class HealthPanel extends JPanel {

    private int userId;
    private HealthDAO dao = new HealthDAO();
    private double userHeight;

    public HealthPanel(int userId) {
        this.userId = userId;
        this.userHeight = dao.getUserHeight(userId);
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // --- Create Tabbed Interface ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setFocusable(false);

        tabbedPane.addTab(" Overview ", buildOverviewPanel());
        tabbedPane.addTab(" Activity Journal ", buildActivityJournalPanel());
        tabbedPane.addTab(" Medications ", buildMedicationsPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // TAB 1: OVERVIEW (DAILY LOGS & CHARTS)
    // ==========================================
    private JPanel buildOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(250, 251, 253));

        java.sql.Date sqlToday = java.sql.Date.valueOf(LocalDate.now());
        HealthDAO.HealthRecord todayRecord = dao.getDailyRecord(userId, sqlToday);

        // --- TOP HEADER & QUICK STATS ---
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(Color.WHITE);
        topSection.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Today's Progress");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        topSection.add(title, BorderLayout.WEST);

        // --- ADVANCED STATS CALCULATION ---
        double kilometers = (todayRecord.steps * 0.75) / 1000.0; 
        double kiloJoules = todayRecord.steps * 0.15; // Base energy from background steps
        
        // Add energy from today's logged activities
        double weightForCalc = todayRecord.weight > 0 ? todayRecord.weight : 70.0;
        List<HealthDAO.ActivityLog> activities = dao.getActivities(userId);
        LocalDate today = LocalDate.now();

        for (HealthDAO.ActivityLog log : activities) {
            if (log.startTime != null && log.startTime.toLocalDateTime().toLocalDate().equals(today)) {
                double met = 4.0; 
                switch(log.activity) {
                    case "Walking":       met = 3.5; break;
                    case "Running":       met = 8.0; break;
                    case "Cycling":       met = 6.0; break;
                    case "Swimming":      met = 7.0; break;
                    case "Weightlifting": met = 3.5; break;
                    case "Yoga":          met = 2.5; break;
                    case "Sports":        met = 6.0; break;
                }
                // Calculate calories, then convert to kJ (1 kcal = 4.184 kJ)
                double kcalBurned = (met * weightForCalc * 3.5) / 200 * log.duration;
                kiloJoules += (kcalBurned * 4.184); 
            }
        }
        
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 0));
        statsRow.setOpaque(false);
        statsRow.add(createQuickStat(String.format("%.2f", kilometers), "km"));
        statsRow.add(createQuickStat(String.format("%.0f", kiloJoules), "kJ"));
        topSection.add(statsRow, BorderLayout.EAST);

        panel.add(topSection, BorderLayout.NORTH);

        // --- CENTRAL DASHBOARD ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // 1. Weekly Goal Row (M T W T F S S)
        mainContent.add(buildWeeklyGoalRow());

        // 2. Main Cards Row (Steps, Sleep, BMI)
        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        cardsRow.setOpaque(false);

        JSpinner stepSpinner = new JSpinner(new SpinnerNumberModel(todayRecord.steps, 0, 100000, 500));
        cardsRow.add(buildRingCard("Steps", stepSpinner, 10000, new Color(66, 133, 244)));
        
        JSpinner sleepSpinner = new JSpinner(new SpinnerNumberModel(todayRecord.sleep, 0.0, 24.0, 0.5));
        cardsRow.add(buildMetricCard("Sleep", sleepSpinner, 8.0, "hrs"));

        // Format Weight to 2 decimal places
        double startingWeight = todayRecord.weight == 0 ? 70.00 : todayRecord.weight;
        JSpinner weightSpinner = new JSpinner(new SpinnerNumberModel(startingWeight, 1.00, 500.00, 0.10));
        JSpinner.NumberEditor weightEditor = new JSpinner.NumberEditor(weightSpinner, "0.00");
        weightSpinner.setEditor(weightEditor);
        cardsRow.add(buildBMICard(weightSpinner));

        mainContent.add(cardsRow);

        // 3. INLINE CHARTS ROW
        JPanel chartsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        chartsRow.setOpaque(false);
        
        chartsRow.add(buildChartWidget("Weekly Steps", "STEPS", new Color(66, 133, 244)));
        chartsRow.add(buildChartWidget("Weekly Sleep", "SLEEP", new Color(120, 94, 240)));
        
        mainContent.add(chartsRow);
        
        // 4. Insights Bar
        mainContent.add(buildInsightBar());

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(250, 251, 253));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- SAVE BUTTON FOOTER ---
        JButton saveBtn = new JButton("Save Today's Log");
        saveBtn.setBackground(new Color(52, 168, 83));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setPreferredSize(new Dimension(220, 45));
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        saveBtn.addActionListener(e -> {
            todayRecord.steps = (Integer) stepSpinner.getValue();
            todayRecord.sleep = (Double) sleepSpinner.getValue();
            // Safely extract the double value from the formatted NumberEditor
            todayRecord.weight = ((Number) weightSpinner.getValue()).doubleValue();
            
            dao.saveDailyRecord(userId, sqlToday, todayRecord);
            JOptionPane.showMessageDialog(this, "Health metrics saved successfully!", "Success", JOptionPane.PLAIN_MESSAGE);
            refreshUI();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(250, 251, 253));
        footer.setBorder(BorderFactory.createEmptyBorder(15, 0, 30, 0));
        footer.add(saveBtn);
        
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createQuickStat(String value, String label) {
        JPanel statPanel = new JPanel(new GridLayout(2, 1));
        statPanel.setOpaque(false);
        JLabel valLabel = new JLabel(value, SwingConstants.CENTER);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JLabel textLabel = new JLabel(label, SwingConstants.CENTER);
        textLabel.setForeground(Color.GRAY);
        statPanel.add(valLabel);
        statPanel.add(textLabel);
        return statPanel;
    }

    private JPanel buildWeeklyGoalRow() {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        rowPanel.setOpaque(false);
        
        Map<LocalDate, Double> weeklySteps = dao.getWeeklyMetric(userId, "STEPS");
        boolean[] goalHit = new boolean[8]; // 1=Mon, 7=Sun
        
        for (Map.Entry<LocalDate, Double> entry : weeklySteps.entrySet()) {
            if (entry.getValue() >= 10000) { 
                int dayOfWeek = entry.getKey().getDayOfWeek().getValue();
                goalHit[dayOfWeek] = true;
            }
        }
        
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        int[] dayIndices = {1, 2, 3, 4, 5, 6, 7}; 

        for (int i = 0; i < days.length; i++) {
            JLabel dayLabel = new JLabel(days[i], SwingConstants.CENTER);
            dayLabel.setPreferredSize(new Dimension(35, 35));
            dayLabel.setOpaque(true);
            dayLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            
            if (goalHit[dayIndices[i]]) {
                dayLabel.setBackground(new Color(66, 133, 244));
                dayLabel.setForeground(Color.WHITE);
                dayLabel.setBorder(BorderFactory.createLineBorder(new Color(66, 133, 244)));
            } else {
                dayLabel.setBackground(Color.WHITE);
                dayLabel.setForeground(new Color(200, 200, 200));
                dayLabel.setBorder(BorderFactory.createLineBorder(new Color(235, 237, 240)));
            }
            rowPanel.add(dayLabel);
        }
        return rowPanel;
    }

    private JPanel buildRingCard(String title, JSpinner spinner, int goal, Color themeColor) {
        JPanel card = createBaseCard(title);
        
        JPanel ringPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int size = 110;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(245, 245, 245));
                g2.drawOval(x, y, size, size);
                
                g2.setColor(themeColor);
                double currentVal = ((Number) spinner.getValue()).doubleValue();
                double angle = (currentVal / goal) * 360;
                g2.draw(new Arc2D.Double(x, y, size, size, 90, -angle, Arc2D.OPEN));
            }
        };
        ringPanel.setOpaque(false);
        ringPanel.setLayout(new GridBagLayout());
        
        spinner.setFont(new Font("Segoe UI", Font.BOLD, 22));
        ringPanel.add(spinner);
        
        // Animate the ring when spinner arrows are clicked
        spinner.addChangeListener(e -> ringPanel.repaint());
        
        card.add(ringPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildMetricCard(String title, JSpinner spinner, double goal, String unit) {
        JPanel card = createBaseCard(title);
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        spinner.setFont(new Font("Segoe UI", Font.BOLD, 28));
        centerPanel.add(spinner);
        card.add(centerPanel, BorderLayout.CENTER);
        
        JLabel targetLabel = new JLabel("Target: " + goal + " " + unit, SwingConstants.CENTER);
        targetLabel.setForeground(Color.GRAY);
        card.add(targetLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildBMICard(JSpinner weightSpinner) {
        JPanel card = createBaseCard("Weight & BMI");
        JPanel innerPanel = new JPanel(new GridLayout(3, 1));
        innerPanel.setOpaque(false);
        
        JLabel bmiLabel = new JLabel("", SwingConstants.CENTER);
        bmiLabel.setFont(new Font("Segoe UI", Font.BOLD, 38));
        
        JLabel categoryLabel = new JLabel();
        categoryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        Runnable updateBMI = () -> {
            double currentWeight = ((Number) weightSpinner.getValue()).doubleValue();
            double currentBmi = currentWeight / Math.pow(userHeight / 100, 2);
            bmiLabel.setText(String.format("%.1f", currentBmi));
            
            if (currentBmi < 18.5) { 
                categoryLabel.setText("Underweight"); 
                categoryLabel.setForeground(Color.ORANGE); 
            } else if (currentBmi < 25) { 
                categoryLabel.setText("Normal"); 
                categoryLabel.setForeground(new Color(52, 168, 83)); 
            } else {
                categoryLabel.setText("Overweight"); 
                categoryLabel.setForeground(Color.RED); 
            }
        };
        
        updateBMI.run(); 
        weightSpinner.addChangeListener(e -> updateBMI.run());
        
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) weightSpinner.getEditor();
        editor.getTextField().setFont(new Font("Segoe UI", Font.BOLD, 18));
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        
        innerPanel.add(weightSpinner);
        innerPanel.add(bmiLabel);
        innerPanel.add(categoryLabel);
        
        card.add(innerPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBaseCard(String titleText) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(280, 230));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.DARK_GRAY);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        card.add(headerPanel, BorderLayout.NORTH);
        return card;
    }

    // ==========================================
    // INLINE CHARTS & INSIGHTS
    // ==========================================

    private JPanel buildChartWidget(String titleText, String dbColumn, Color chartColor) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setPreferredSize(new Dimension(430, 240));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.DARK_GRAY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        wrapper.add(title, BorderLayout.NORTH);

        Map<LocalDate, Double> data = dao.getWeeklyMetric(userId, dbColumn);
        BarChartPanel chartPanel = new BarChartPanel(data, chartColor);
        wrapper.add(chartPanel, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel buildInsightBar() {
        JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.setBackground(Color.WHITE);
        barPanel.setMaximumSize(new Dimension(900, 80));
        barPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true),
            BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));

        int streak = dao.getStepStreak(userId);
        String message = (streak > 0) 
            ? "🔥 You're on a " + streak + "-day streak for steps! Keep it up!" 
            : "💡 Start your activity streak today by reaching your step goal!";
        
        JLabel insightText = new JLabel(message);
        insightText.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        insightText.setForeground(new Color(66, 133, 244));
        
        JLabel iconLabel = new JLabel("💡 ");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        
        barPanel.add(iconLabel, BorderLayout.WEST);
        barPanel.add(insightText, BorderLayout.CENTER);
        return barPanel;
    }

    private class BarChartPanel extends JPanel {
        private Map<LocalDate, Double> data;
        private Color barColor;

        public BarChartPanel(Map<LocalDate, Double> data, Color barColor) {
            this.data = data;
            this.barColor = barColor;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 30; 

            double max = 0;
            for (Double val : data.values()) {
                if (val > max) max = val;
            }
            if (max == 0) max = 10; 

            g2.setColor(new Color(240, 240, 240));
            for (int i = 0; i <= 4; i++) {
                int y = padding + (i * (height - 2 * padding) / 4);
                g2.drawLine(padding, y, width - padding, y);
            }

            int numBars = data.size();
            int barWidth = (width - 2 * padding) / (numBars * 2);
            int xOffset = padding + barWidth / 2;

            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("E"); 

            for (Map.Entry<LocalDate, Double> entry : data.entrySet()) {
                double value = entry.getValue();
                int barHeight = (int) ((value / max) * (height - 2 * padding));
                
                g2.setColor(barColor);
                g2.fillRoundRect(xOffset, height - padding - barHeight, barWidth, barHeight, 8, 8);

                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String valStr = (value == Math.floor(value)) ? String.format("%.0f", value) : String.valueOf(value);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(valStr, xOffset + (barWidth - fm.stringWidth(valStr)) / 2, height - padding - barHeight - 5);

                g2.setColor(Color.GRAY);
                String dayStr = entry.getKey().format(dayFormatter);
                g2.drawString(dayStr, xOffset + (barWidth - fm.stringWidth(dayStr)) / 2, height - padding + 15);

                xOffset += barWidth * 2;
            }
        }
    }

    // ==========================================
    // TAB 2: ACTIVITY JOURNAL
    // ==========================================
    private JPanel buildActivityJournalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(250, 250, 252));
        
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        refreshActivityList(listPanel); 
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(350, 0));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 5, 8, 5);

        JLabel formTitle = new JLabel("Log Activity");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        formTitle.setForeground(new Color(52, 168, 83)); 
        gbc.gridy = 0; 
        formPanel.add(formTitle, gbc);

        gbc.gridy++; 
        formPanel.add(new JLabel("Activity Type:"), gbc);
        
        String[] activities = {"Walking", "Running", "Cycling", "Swimming", "Weightlifting", "Yoga", "Sports"};
        JComboBox<String> activityBox = new JComboBox<>(activities);
        activityBox.setBackground(Color.WHITE);
        gbc.gridy++; 
        formPanel.add(activityBox, gbc);

        gbc.gridy++; 
        formPanel.add(new JLabel("Date:"), gbc);
        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd MMM yyyy"));
        gbc.gridy++; 
        formPanel.add(dateSpinner, gbc);

        JPanel timeBox = new JPanel(new GridLayout(2, 2, 10, 2));
        timeBox.setBackground(Color.WHITE);
        timeBox.add(new JLabel("Start Time:"));
        timeBox.add(new JLabel("End Time:"));
        
        SpinnerDateModel startModel = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
        JSpinner startSpinner = new JSpinner(startModel);
        startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "HH:mm"));
        timeBox.add(startSpinner);
        
        SpinnerDateModel endModel = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
        JSpinner endSpinner = new JSpinner(endModel);
        endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "HH:mm"));
        timeBox.add(endSpinner);
        
        gbc.gridy++; 
        formPanel.add(timeBox, gbc);

        JButton addBtn = new JButton("Save to Journal");
        addBtn.setBackground(new Color(52, 168, 83)); 
        addBtn.setForeground(Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        
        addBtn.addActionListener(e -> {
            String activity = (String) activityBox.getSelectedItem();
            Date selectedDate = (Date) dateSpinner.getValue();
            Date startTime = (Date) startSpinner.getValue();
            Date endTime = (Date) endSpinner.getValue();

            Calendar calendarDate = Calendar.getInstance(); 
            calendarDate.setTime(selectedDate);
            
            Calendar calendarStart = Calendar.getInstance(); 
            calendarStart.setTime(startTime);
            calendarStart.set(calendarDate.get(Calendar.YEAR), calendarDate.get(Calendar.MONTH), calendarDate.get(Calendar.DAY_OF_MONTH));
            
            Calendar calendarEnd = Calendar.getInstance(); 
            calendarEnd.setTime(endTime);
            calendarEnd.set(calendarDate.get(Calendar.YEAR), calendarDate.get(Calendar.MONTH), calendarDate.get(Calendar.DAY_OF_MONTH));

            if (calendarStart.getTime().after(new Date()) || calendarEnd.getTime().after(new Date())) {
                JOptionPane.showMessageDialog(panel, "Cannot log activities in the future!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!calendarEnd.after(calendarStart)) {
                JOptionPane.showMessageDialog(panel, "End time must be after Start time.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            long differenceInMillis = calendarEnd.getTimeInMillis() - calendarStart.getTimeInMillis();
            int durationMins = (int) (differenceInMillis / (60 * 1000));

            dao.addActivity(userId, activity, durationMins, new java.sql.Timestamp(calendarStart.getTimeInMillis()), new java.sql.Timestamp(calendarEnd.getTimeInMillis()));
            refreshActivityList(listPanel);
        });

        gbc.gridy++; 
        gbc.insets = new Insets(20, 5, 10, 5);
        formPanel.add(addBtn, gbc);

        panel.add(formPanel, BorderLayout.EAST);
        return panel;
    }

    private void refreshActivityList(JPanel listPanel) {
        listPanel.removeAll();
        List<HealthDAO.ActivityLog> logs = dao.getActivities(userId);
        
        java.sql.Date sqlToday = java.sql.Date.valueOf(LocalDate.now());
        double userWeight = dao.getDailyRecord(userId, sqlToday).weight;
        if (userWeight <= 0) userWeight = 70.0; 

        if (logs.isEmpty()) {
            JLabel emptyLabel = new JLabel("No activities logged yet. Get moving!");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(emptyLabel);
        } else {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("dd MMM, HH:mm");

            for (HealthDAO.ActivityLog log : logs) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true),
                    BorderFactory.createEmptyBorder(15, 20, 15, 20)
                ));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

                double metValue = 4.0; 
                String emojiIcon = "🏃";
                
                switch(log.activity) {
                    case "Walking":       metValue = 3.5; emojiIcon = "🚶"; break;
                    case "Running":       metValue = 8.0; emojiIcon = "🏃"; break;
                    case "Cycling":       metValue = 6.0; emojiIcon = "🚴"; break;
                    case "Swimming":      metValue = 7.0; emojiIcon = "🏊"; break;
                    case "Weightlifting": metValue = 3.5; emojiIcon = "🏋️"; break;
                    case "Yoga":          metValue = 2.5; emojiIcon = "🧘"; break;
                    case "Sports":        metValue = 6.0; emojiIcon = "🏀"; break;
                }
                
                int caloriesBurned = (int) Math.round((metValue * userWeight * 3.5) / 200 * log.duration);

                JPanel textPanel = new JPanel(new GridLayout(2, 1));
                textPanel.setOpaque(false);
                
                JLabel titleLabel = new JLabel(emojiIcon + " " + log.activity + " (" + log.duration + " mins)");
                titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                titleLabel.setForeground(new Color(50, 50, 50));
                
                String timeRange = (log.startTime != null && log.endTime != null) 
                    ? timeFormat.format(log.startTime) + "  →  " + timeFormat.format(log.endTime) 
                    : "N/A";
                    
                JLabel detailsLabel = new JLabel("🔥 ~" + caloriesBurned + " kcal burned  |  🕒 " + timeRange);
                detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                detailsLabel.setForeground(Color.GRAY);
                
                textPanel.add(titleLabel);
                textPanel.add(detailsLabel);

                JButton deleteBtn = new JButton("X");
                deleteBtn.setForeground(Color.RED);
                deleteBtn.setContentAreaFilled(false);
                deleteBtn.setBorderPainted(false);
                deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                deleteBtn.addActionListener(e -> {
                    dao.deleteActivity(log.logId);
                    refreshActivityList(listPanel);
                });

                card.add(textPanel, BorderLayout.CENTER);
                card.add(deleteBtn, BorderLayout.EAST);

                listPanel.add(card);
                listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    // ==========================================
    // TAB 3: MEDICATIONS
    // ==========================================
    private JPanel buildMedicationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(250, 250, 252));
        
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        refreshMedicationList(listPanel); 
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(350, 0));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 5, 8, 5);

        JLabel formTitle = new JLabel("Add Medication");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        formTitle.setForeground(new Color(100, 149, 237));
        gbc.gridy = 0; 
        formPanel.add(formTitle, gbc);

        gbc.gridy++; 
        formPanel.add(new JLabel("Medication Name:"), gbc);
        JTextField nameField = new JTextField();
        gbc.gridy++; 
        formPanel.add(nameField, gbc);

        gbc.gridy++; 
        formPanel.add(new JLabel("Dosage (e.g. 1 pill, 50mg):"), gbc);
        JTextField dosageField = new JTextField();
        gbc.gridy++; 
        formPanel.add(dosageField, gbc);

        gbc.gridy++; 
        formPanel.add(new JLabel("Start Date:"), gbc);
        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd MMM yyyy"));
        gbc.gridy++; 
        formPanel.add(dateSpinner, gbc);

        gbc.gridy++; 
        formPanel.add(new JLabel("Interval (Hours between doses):"), gbc);
        JSpinner gapSpinner = new JSpinner(new SpinnerNumberModel(24, 1, 168, 1)); 
        gbc.gridy++; 
        formPanel.add(gapSpinner, gbc);

        JButton addBtn = new JButton("Add to Regimen");
        addBtn.setBackground(new Color(100, 149, 237));
        addBtn.setForeground(Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        
        addBtn.addActionListener(e -> {
            String medName = nameField.getText().trim();
            String medDosage = dosageField.getText().trim();
            int intervalGap = (Integer) gapSpinner.getValue();
            Date startDate = (Date) dateSpinner.getValue();

            if (medName.isEmpty() || medDosage.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Name and Dosage cannot be empty.", "Error", JOptionPane.PLAIN_MESSAGE);
                return;
            }

            dao.addMedication(userId, medName, medDosage, new java.sql.Date(startDate.getTime()), intervalGap);
            
            nameField.setText("");
            dosageField.setText("");
            gapSpinner.setValue(24);
            refreshMedicationList(listPanel);
        });

        gbc.gridy++; 
        gbc.insets = new Insets(20, 5, 10, 5);
        formPanel.add(addBtn, gbc);

        panel.add(formPanel, BorderLayout.EAST);
        return panel;
    }

    private void refreshMedicationList(JPanel listPanel) {
        listPanel.removeAll();
        List<HealthDAO.Medication> medications = dao.getMedications(userId);

        if (medications.isEmpty()) {
            JLabel emptyLabel = new JLabel("No medications added to your regimen.");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(emptyLabel);
        } else {
            for (HealthDAO.Medication med : medications) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true),
                    BorderFactory.createEmptyBorder(15, 20, 15, 20)
                ));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

                JPanel textPanel = new JPanel(new GridLayout(2, 1));
                textPanel.setOpaque(false);
                
                JLabel nameLabel = new JLabel(med.name + " (" + med.dosage + ")");
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
                nameLabel.setForeground(new Color(50, 50, 50));
                
                JLabel detailLabel = new JLabel("Take every " + med.gap + " hours");
                detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                detailLabel.setForeground(Color.GRAY);
                
                textPanel.add(nameLabel);
                textPanel.add(detailLabel);

                JButton deleteBtn = new JButton("X");
                deleteBtn.setForeground(Color.RED);
                deleteBtn.setContentAreaFilled(false);
                deleteBtn.setBorderPainted(false);
                deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                deleteBtn.addActionListener(e -> {
                    dao.deleteMedication(med.medId);
                    refreshMedicationList(listPanel);
                });

                card.add(textPanel, BorderLayout.CENTER);
                card.add(deleteBtn, BorderLayout.EAST);

                listPanel.add(card);
                listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void refreshUI() {
        this.removeAll();
        this.add(new HealthPanel(this.userId)); 
        this.revalidate();
        this.repaint();
    }
}