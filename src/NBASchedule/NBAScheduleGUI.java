package NBASchedule;

import javax.swing.*;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class NBAScheduleGUI {
    private JFrame frame;
    private JComboBox<IconItem> teamComboBox;
    private JTextArea textArea;
    private Map<String, List<GameDetails>> schedule;
    private JTable table;
    private JLabel loadingLabel;
    private DefaultTableModel tableModel;
    DefaultComboBoxModel<IconItem> model;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NBAScheduleGUI gui = new NBAScheduleGUI();
            gui.frame.setVisible(true);
        });
    }

    public NBAScheduleGUI() {
    	schedule = new HashMap<>();
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);


        JLabel header = new JLabel("NBA Schedule Viewer");
        header.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.setHorizontalAlignment(JLabel.CENTER);
        frame.add(header, BorderLayout.PAGE_START);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

    
        panel.setBackground(Color.LIGHT_GRAY);

        model = new DefaultComboBoxModel<>();
        teamComboBox = new JComboBox<>(model);
        model.addElement(new IconItem("All Teams", new ImageIcon("")));
        teamComboBox.setRenderer(new IconListRenderer());
        teamComboBox.setFont(new Font("SansSerif", Font.PLAIN, 18));
        teamComboBox.addActionListener(e -> {
            IconItem selected = (IconItem) teamComboBox.getSelectedItem();
            filterSchedule(selected.text);
        });

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        tableModel.addColumn("Game");
        table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 16));
        table.setRowHeight(25); 
        table.setGridColor(Color.BLACK); 
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 18));  
        table.setDefaultRenderer(Object.class, new IconTableCellRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(teamComboBox, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        frame.add(panel);


        loadingLabel = new JLabel("Loading...");
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(loadingLabel, BorderLayout.PAGE_END);
        
        String[] teams = {"ATL", "BOS", "BKN", "CHA", "CHI", "CLE", "DAL", "DEN", "DET", "GSW", "HOU", "IND", "LAC", "LAL", "MEM", "MIA", "MIL", "MIN", "NOP", "NYK", "OKC", "ORL", "PHI", "PHX", "POR", "SAC", "SAS", "TOR", "UTA", "WAS"};
        for (String team : teams) {
            Image icon = new ImageIcon("C:\\Users\\vash\\eclipse-workspace\\NBAScheduleViewer\\src\\NBASchedule\\images\\" + team + ".png").getImage();
            Image newImage = icon.getScaledInstance(20, 20, Image.SCALE_SMOOTH); 
            model.addElement(new IconItem(team, new ImageIcon(newImage)));
        }
        
        fetchNBASchedule();
    }

    private class IconItem {
        final String text;
        final Icon icon;
        final GameDetails gameDetails;

        IconItem(String text, Icon icon) {
            this(text, icon, null);
        }

        IconItem(String text, Icon icon, GameDetails gameDetails) {
            this.text = text;
            this.icon = icon;
            this.gameDetails = gameDetails;
        }
    }


    private class IconListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            IconItem item = (IconItem) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, item.text, index, isSelected, cellHasFocus);
            label.setIcon(item.icon);
            return label;
        }
    }

    public void fetchNBASchedule() {
        new Thread(() -> {
            try {
                URL url = new URL("https://cdn.nba.com/static/json/staticData/scheduleLeagueV2.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject leagueSchedule = jsonObject.getJSONObject("leagueSchedule");
                JSONArray gameDates = leagueSchedule.getJSONArray("gameDates");

                for (int j = 0; j < gameDates.length(); j++) {
                    JSONObject gameDate = gameDates.getJSONObject(j);
                    JSONArray games = gameDate.getJSONArray("games");

                    for (int i = 0; i < games.length(); i++) {
                        JSONObject game = games.getJSONObject(i);
                        JSONObject homeTeam = game.getJSONObject("homeTeam");
                        JSONObject awayTeam = game.getJSONObject("awayTeam");

                        String homeTeamCode = homeTeam.getString("teamTricode");
                        String awayTeamCode = awayTeam.getString("teamTricode");
                        String date = game.getString("gameDateEst").substring(0, 10);                      
                        		String gameStatus = game.getString("gameStatusText");  // Extract gameStatusText

                        		String location = game.getString("arenaName") + " in " + game.getString("arenaCity") + ", "+ game.getString("arenaState");  
                        		String time = game.getString("gameStatusText");  

                        		GameDetails gameDetailsHome = new GameDetails(homeTeamCode, awayTeamCode, date, location, time);
                        		GameDetails gameDetailsAway = new GameDetails(awayTeamCode, homeTeamCode, date, location, time);

                        		schedule.computeIfAbsent(homeTeamCode, k -> new ArrayList<>()).add(gameDetailsHome);
                        		schedule.computeIfAbsent(awayTeamCode, k -> new ArrayList<>()).add(gameDetailsAway);

                    }
                }

                filterSchedule("All Teams");  

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    
    private class IconTableCellRenderer extends JLabel implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        	 if (value instanceof IconItem) {
                 IconItem item = (IconItem) value;
                 setText(item.text);
                 setIcon(item.icon);
             } else {
                 setText(value.toString());
                 setIcon(null);
             }
            if (row % 2 == 0) {
                setBackground(Color.WHITE);
            } else {
                setBackground(new Color(220, 220, 220));
            }
            setOpaque(true);
            return this;
        }
    }
    
    
    public void filterSchedule(String team) {
        tableModel.setRowCount(0); 
        List<GameDetails> games = schedule.getOrDefault(team, new ArrayList<>());
        if(team.equals("All Teams")) {
        	loadingLabel.setText("Loading...");
        }else {
        	loadingLabel.setText(team);
        }
        
        for (GameDetails gameDetails : games) {
            String opponent = gameDetails.awayTeam;
            Icon opponentIcon = null;

            for (int i = 0; i < model.getSize(); i++) {
                IconItem item = model.getElementAt(i);
                
                if (item.text.equals(opponent)) {
                    opponentIcon = item.icon;
                    break;
                }
            }

            String gameDetailString = "vs " + gameDetails.awayTeam + " on " + gameDetails.date + " at " + gameDetails.time;
            tableModel.addRow(new Object[]{new IconItem(gameDetailString, opponentIcon, gameDetails)});
        }
    
        
        for (MouseListener ml : table.getMouseListeners()) {
            table.removeMouseListener(ml);
        }
        
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
            	if(e.getClickCount() == 1) {
	                int row = table.rowAtPoint(e.getPoint());
	                int col = table.columnAtPoint(e.getPoint());
	
	                if (row >= 0 && col >= 0) {
	                    IconItem item = (IconItem) tableModel.getValueAt(row, 0);
	                    GameDetails gameDetails = item.gameDetails;
	                    
	                 
	                    JDialog gameDetailDialog = new JDialog(frame, "Game Details", true);
	                    gameDetailDialog.setSize(300, 200);
	                    gameDetailDialog.setLayout(new GridLayout(5, 1));  
	                    
	                   
	                    JLabel homeTeamLabel = new JLabel("Home Team: " + gameDetails.homeTeam);
	                    JLabel awayTeamLabel = new JLabel("Away Team: " + gameDetails.awayTeam);
	                    JLabel dateLabel = new JLabel("Date: " + gameDetails.date);
	                    JLabel locationLabel = new JLabel("Location: " + gameDetails.location);
	                    JLabel statusLabel = new JLabel("Time: " + gameDetails.time);
	                    
	                   
	                    gameDetailDialog.add(homeTeamLabel);
	                    gameDetailDialog.add(awayTeamLabel);
	                    gameDetailDialog.add(dateLabel);
	                    gameDetailDialog.add(locationLabel);
	                    gameDetailDialog.add(statusLabel);
	                    
	                    gameDetailDialog.setVisible(true);
	                }
	            }
            }
        });


}

class GameDetails {
    String homeTeam;
    String awayTeam;
    String date;
    String location;
    String time;

    

    public GameDetails(String homeTeam, String awayTeam, String date, String location, String time) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.date = date;
        this.location = location;
        this.time = time;
    }
}
}