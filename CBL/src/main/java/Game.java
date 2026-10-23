import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Game {

    private static List<String> readLevelFile(String filename) {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filename))) {
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
        } catch (IOException e) {
            
            System.out.println("Error reading " + filename);
        }
        return lines;
    }

    public static void main(String[] args) {
        List<String> mapLines = readLevelFile("level.txt");
        if (mapLines.isEmpty()) {
            System.out.println("Map is empty!");
            return;
        }

        Level level = new Level(mapLines);
        GameLogic game = new GameLogic(level);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dungeon");
            GamePanel panel = new GamePanel(game);
            frame.add(panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}

class Position {
    private final int row;
    private final int col;

    static final Position UP = new Position(-1, 0);
    static final Position DOWN = new Position(1, 0);
    static final Position LEFT = new Position(0, -1);
    static final Position RIGHT = new Position(0, 1);
    private static final Position[] DIRS = {UP, DOWN, LEFT, RIGHT};

    Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    int getRow() {
        return row;
    }

    int getCol() {
        return col;
    }

    static Position randomDirection(Random rng) {
        return DIRS[rng.nextInt(DIRS.length)];
    }

    boolean sameAs(Position other) {
        if (other == null) {
            return false;
        }
        return this.row == other.row && this.col == other.col;
    }

    Position add(Position other) {
        return new Position(this.row + other.row, this.col + other.col);
    }

    private int getSign(int val) {
        if (val > 0) {
            return 1;
        } else if (val < 0) {
            return -1;
        }
        return 0;
    }

    Position stepTowards(Position target) {
        int dy = target.row - this.row;
        int dx = target.col - this.col;
        if (Math.abs(dy) > Math.abs(dx)) {
            return new Position(getSign(dy), 0);
        }
        return new Position(0, getSign(dx));
    }

    boolean isNear(Position other, int dist) {
        int dr = Math.abs(this.row - other.row);
        int dc = Math.abs(this.col - other.col);
        return dr <= dist && dc <= dist;
    }
}

enum EnemyType {
    EXAM('E', 2, 3, 1),
    SECURITY('C', 4, 2, 1),
    PROFESSOR('P', 3, 4, 2);

    private final char symbol;
    private final int hp;
    private final int damage;
    private final int moves;

    EnemyType(char symbol, int hp, int damage, int moves) {
        this.symbol = symbol;
        this.hp = hp;
        this.damage = damage;
        this.moves = moves;
    }

    char getSymbol() {
        return symbol;
    }

    int getHp() {
        return hp;
    }

    int getDamage() {
        return damage;
    }

    int getMoves() {
        return moves;
    }

    static EnemyType fromChar(char c) {
        for (EnemyType type : values()) {
            if (type.symbol == c) {
                return type;
            }
        }
        return null;
    }
}

class Enemy {
    private Position pos;
    private int hp;
    private EnemyType type;

    Enemy(EnemyType type, Position startPos) {
        this.type = type;
        this.pos = startPos;
        this.hp = type.getHp();
    }

    Position getPos() {
        return pos;
    }

    EnemyType getType() {
        return type;
    }

    private Position getNextMove(Position playerPos, Random rng) {
        if (pos.isNear(playerPos, GameLogic.SIGHT) && rng.nextBoolean()) {
            return pos.stepTowards(playerPos);
        }
        
        if (type == EnemyType.SECURITY) {
            return null;
        }
        return Position.randomDirection(rng);
    }

    int update(Level level, Position playerPos, Random rng) {
        for (int i = 0; i < type.getMoves(); i++) {
            Position step = getNextMove(playerPos, rng);
            if (step == null) {
                return 0;
            }
            Position nextPos = pos.add(step);
            if (nextPos.sameAs(playerPos)) {
                
                return type.getDamage();
            }
            if (!level.canEnemyMoveTo(nextPos)) {
                return 0;
            }
            pos = nextPos;
        }
        return 0;
    }

    boolean takeHit(int dmg) {
        hp -= dmg;
        return hp <= 0;
    }
}

class Player {
    private Position pos;
    private int keys = 0;
    private int swords = 0;
    private int shields = 0;
    private int resits = 0;
    static final int MAX_HP = 60;
    private int health = MAX_HP;

    Player(Position startPos) {
        this.pos = startPos;
    }

    Position getPos() {
        return pos;
    }

    int getKeys() {
        return keys;
    }

    int getShields() {
        return shields;
    }

    int getResits() {
        return resits;
    }

    int getHealth() {
        return health;
    }

    void moveTo(Position p) {
        this.pos = p;
    }

    int getAttack() {
        return 1 + swords;
    }

    void useResit() {
        if (resits > 0) {
            resits--;
            health = Math.min(MAX_HP, health + 7);
            
        }
    }

    void pickUp(char item) {
        if (item == '+') {
            resits++;
        } else if (item == 'S') {
            swords++;
        } else if (item == 'O') {
            shields++;
        } else if (item == 'K') {
            keys++;
        }
    }

    void takeDamage(int dmg) {
        int actualDmg = Math.max(1, dmg - shields);
        health -= actualDmg;
    }
}

class Level {
    private char[][] map;
    private boolean[][] seen;
    private Position startPos;
    private List<String> rawLines;
    private List<Enemy> enemies = new ArrayList<>();

    Level(List<String> lines) {
        this.rawLines = lines;
    }

    Position getStartPos() {
        return startPos;
    }

    List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    void removeEnemy(Enemy e) {
        enemies.remove(e);
    }

    boolean inBounds(Position p) {
        return p.getRow() >= 0 && p.getRow() < map.length
                && p.getCol() >= 0 && p.getCol() < map[p.getRow()].length;
    }

    void load() {
        enemies.clear();
        map = new char[rawLines.size()][];
        seen = new boolean[map.length][];

        for (int r = 0; r < map.length; r++) {
            String line = rawLines.get(r);
            map[r] = line.toCharArray();
            seen[r] = new boolean[map[r].length];
            setupRow(r);
        }
    }

    private void setupRow(int r) {
        for (int c = 0; c < map[r].length; c++) {
            char ch = map[r][c];
            EnemyType et = EnemyType.fromChar(ch);
            if (et != null) {
                enemies.add(new Enemy(et, new Position(r, c)));
                map[r][c] = '.';
            } else if (ch == '@') {
                startPos = new Position(r, c);
                map[r][c] = '.';
            }
        }
    }

    Enemy getEnemyAt(Position p) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.getPos().sameAs(p)) {
                return e;
            }
        }
        return null;
    }

    char getTile(Position p) {
        if (!inBounds(p)) {
            return ' ';
        }
        return map[p.getRow()][p.getCol()];
    }

    char getCell(Position p) {
        char t = getTile(p);
        return t == ' ' ? '#' : t;
    }

    void clearTile(Position p) {
        map[p.getRow()][p.getCol()] = '.';
    }

    boolean isExplored(Position p) {
        return inBounds(p) && seen[p.getRow()][p.getCol()];
    }

    boolean canEnemyMoveTo(Position p) {
        return getCell(p) == '.' && getEnemyAt(p) == null;
    }

    // fog of war reveal
    void revealArea(Position center, int radius) {
        int startR = Math.max(0, center.getRow() - radius);
        int endR = Math.min(map.length - 1, center.getRow() + radius);

        for (int r = startR; r <= endR; r++) {
            int startC = Math.max(0, center.getCol() - radius);
            int endC = Math.min(map[r].length - 1, center.getCol() + radius);
            for (int c = startC; c <= endC; c++) {
                seen[r][c] = true;
            }
        }
    }
}

class GameLogic {
    private Level level;
    private Player player;
    private Random rng = new Random();

    static final int SIGHT = 3;
    static final int REQ_KEYS = 4;

    private boolean won;
    private boolean godMode;
    private boolean playerHit;
    private boolean enemyKilled;
    private Position combatPos;

    GameLogic(Level level) {
        this.level = level;
        reset();
    }

    Level getLevel() {
        return level;
    }

    Player getPlayer() {
        return player;
    }

    boolean isWon() {
        return won;
    }

    boolean isGodMode() {
        return godMode;
    }

    boolean isPlayerHit() {
        return playerHit;
    }

    boolean isEnemyKilled() {
        return enemyKilled;
    }

    Position getCombatPos() {
        return combatPos;
    }

    void toggleGodMode() {
        godMode = !godMode;
    }

    void reset() {
        level.load();
        player = new Player(level.getStartPos());
        won = false;
        godMode = false;
        playerHit = false;
        combatPos = null;
        level.revealArea(player.getPos(), SIGHT);
    }

    boolean isGameOver() {
        return won || player.getHealth() <= 0;
    }

    private boolean isArrow(char tile) {
        if (tile == 'v' || tile == '^') {
            return true;
        }
        return tile == '<' || tile == '>';
    }

    private boolean isWallOrLocked(char tile) {
        if (tile == '#') {
            return true;
        }
        return tile == 'D' && player.getKeys() < REQ_KEYS;
    }

    void movePlayer(Position dir) {
        playerHit = false;
        combatPos = null;
        Position target = player.getPos().add(dir);

        if (!tryStep(target)) {
            return;
        }
        level.revealArea(player.getPos(), SIGHT);
        if (!won) {
            updateEnemies();
        }
    }

    private boolean tryStep(Position target) {
        Enemy enemy = level.getEnemyAt(target);
        if (enemy != null) {
            fight(enemy, target);
            return true;
        }

        char tile = level.getCell(target);
        if (isWallOrLocked(tile)) {
            return false;
        }

        if (tile == 'D') {
            won = true;
            
        }
        player.pickUp(tile);

        // dont delete the arrows on the ground
        if (!isArrow(tile)) {
            level.clearTile(target);
        }
        player.moveTo(target);
        return true;
    }

    private void fight(Enemy enemy, Position target) {
        combatPos = target;
        enemyKilled = enemy.takeHit(player.getAttack());
        if (enemyKilled) {
            level.removeEnemy(enemy);
        } else {
            damagePlayer(enemy.getType().getDamage());
        }
    }

    void useItem(int index) {
        playerHit = false;
        combatPos = null;
        if (index < player.getResits()) {
            player.useResit();
            updateEnemies();
        }
    }

    private void updateEnemies() {
        List<Enemy> enemies = level.getEnemies();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.getPos().sameAs(combatPos)) {
                int dmg = e.update(level, player.getPos(), rng);
                damagePlayer(dmg);
            }
        }
    }

    private void damagePlayer(int dmg) {
        if (dmg > 0 && !godMode) {
            player.takeDamage(dmg);
            playerHit = true;
        }
    }
}

class GamePanel extends JPanel {
    private GameLogic game;
    private Map<Character, BufferedImage> sprites = new HashMap<>();
    private BufferedImage menuBg;
    private Map<Integer, Position> keyMap = new HashMap<>();

    private boolean showMenu = true;
    private boolean showCredits = false;

    private static final int TILE = 64;
    private static final int VIEW_R = 6;
    private static final int VIEW_C = 9;
    private static final int MAP_H = (VIEW_R * 2 + 1) * TILE;
    private static final int HUD_H = 60;

    
    private static final String[] MENU_TEXT = {
      "Press ENTER or SPACE to Start",
      "Press C for Credits",
      "Controls: WASD / Arrows to Move | 1-9 for items | '+' God Mode"
    };
    private static final String[] CREDITS_TEXT = {
      "Lead Developers: Jakov Perčin, Maksim Popović",
      "Level Designer: Jakov Perčin",
      "Enemy AI: Maksim Popović"
    };

    GamePanel(GameLogic game) {
        this.game = game;
        this.menuBg = loadImg("menubg");
        initControls();
        loadSprites();

        int width = (VIEW_C * 2 + 1) * TILE;
        setPreferredSize(new Dimension(width, MAP_H + HUD_H));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKeyPress(e.getKeyCode());
            }
        });
    }

    private static BufferedImage loadImg(String name) {
        try {
            return ImageIO.read(new File("textures/" + name + ".png"));
        } catch (IOException e) {
            System.out.println("Missing texture: " + name);
            return null;
        }
    }

    private void loadSprites() {
        sprites.put('@', loadImg("player"));
        sprites.put('#', loadImg("wall"));
        sprites.put('.', loadImg("floor"));
        sprites.put(' ', loadImg("fog"));
        sprites.put('E', loadImg("enemy"));
        sprites.put('C', loadImg("security"));
        sprites.put('P', loadImg("professor"));
        sprites.put('+', loadImg("item"));
        sprites.put('K', loadImg("key"));
        sprites.put('D', loadImg("door"));
        sprites.put('S', loadImg("sword"));
        sprites.put('O', loadImg("shield"));
        sprites.put('v', loadImg("arrow_down"));
        sprites.put('^', loadImg("arrow_up"));
        sprites.put('<', loadImg("arrow_left"));
        sprites.put('>', loadImg("arrow_right"));
    }

    private void initControls() {
        keyMap.put(KeyEvent.VK_W, Position.UP);
        keyMap.put(KeyEvent.VK_UP, Position.UP);
        keyMap.put(KeyEvent.VK_S, Position.DOWN);
        keyMap.put(KeyEvent.VK_DOWN, Position.DOWN);
        keyMap.put(KeyEvent.VK_A, Position.LEFT);
        keyMap.put(KeyEvent.VK_LEFT, Position.LEFT);
        keyMap.put(KeyEvent.VK_D, Position.RIGHT);
        keyMap.put(KeyEvent.VK_RIGHT, Position.RIGHT);
    }

    private void onKeyPress(int code) {
        
        if (showCredits) {
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_B) {
                showCredits = false;
            }
        } else if (showMenu) {
            handleMenuInput(code);
        } else {
            handleGameInput(code);
        }
        repaint();
    }

    private void handleMenuInput(int code) {
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
            showMenu = false;
        } else if (code == KeyEvent.VK_C) {
            showCredits = true;
        }
    }

    private void handleGameInput(int code) {
        if (game.isGameOver()) {
            if (code == KeyEvent.VK_R) {
                game.reset();
            }
            return;
        }
        processTurnInput(code);
    }

    private boolean isGodModeKey(int code) {
        return code == KeyEvent.VK_PLUS
                || code == KeyEvent.VK_ADD
                || code == KeyEvent.VK_EQUALS;
    }

    private void processTurnInput(int code) {
        Position dir = keyMap.get(code);
        if (dir != null) {
            game.movePlayer(dir);
        } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            game.useItem(code - KeyEvent.VK_1);
        } else if (isGodModeKey(code)) {
            game.toggleGodMode();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (showMenu) {
            renderMenu(g);
        } else {
            renderGame(g);
            renderHud(g);
        }
    }

    private char getDisplayChar(Position pos) {
        Position playerPos = game.getPlayer().getPos();
        Level level = game.getLevel();
        if (pos.sameAs(playerPos)) {
            return '@';
        }
        if (!level.isExplored(pos)) {
            return ' ';
        }
        Enemy en = level.getEnemyAt(pos);
        if (en != null && pos.isNear(playerPos, GameLogic.SIGHT)) {
            return en.getType().getSymbol();
        }
        return level.getTile(pos);
    }

    private void drawSprite(Graphics g, char symbol, int x, int y) {
        BufferedImage img = sprites.get(symbol);
        if (img != null) {
            g.drawImage(img, x * TILE, y * TILE, TILE, TILE, null);
        } else if (symbol != ' ') {
            
            g.setColor(Color.GRAY);
            g.fillRect(x * TILE, y * TILE, TILE, TILE);
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(symbol), x * TILE + 24, y * TILE + 36);
        }
    }

    private void drawCell(Graphics g, Position pos, int x, int y) {
        char ch = getDisplayChar(pos);
        if (ch != '#' && ch != ' ') {
            drawSprite(g, '.', x, y);
        }
        drawSprite(g, ch, x, y);

        if (pos.sameAs(game.getCombatPos())) {
            if (game.isEnemyKilled()) {
                g.setColor(new Color(255, 215, 0, 120));
            } else {
                g.setColor(new Color(255, 255, 255, 120));
            }
            g.fillRect(x * TILE, y * TILE, TILE, TILE);
        }
    }

    private void renderGame(Graphics g) {
        Position pPos = game.getPlayer().getPos();
        for (int y = 0; y <= VIEW_R * 2; y++) {
            for (int x = 0; x <= VIEW_C * 2; x++) {
                Position mapPos = new Position(pPos.getRow() + y - VIEW_R, pPos.getCol() + x - VIEW_C);
                drawCell(g, mapPos, x, y);
            }
        }
        if (game.isPlayerHit()) {
            g.setColor(new Color(255, 0, 0, 70));
            g.fillRect(0, 0, getWidth(), MAP_H);
        }
    }

    private String buildStatusString() {
        Player p = game.getPlayer();
        String hpStr = game.isGodMode() ? "INF" : String.valueOf(Math.max(0, p.getHealth()));
        String status = "ECTS " + hpStr + "/" + Player.MAX_HP
                + (game.isGodMode() ? " [GOD MODE]" : "")
                + "  |  Keys " + p.getKeys() + "/" + GameLogic.REQ_KEYS
                + "  |  ATK " + p.getAttack() + "  |  DEF " + p.getShields()
                + "  |  Items:";

        for (int i = 1; i <= p.getResits(); i++) {
            status += " " + i + "=resit";
        }
        return status;
    }

    private void renderHud(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, MAP_H, getWidth(), HUD_H);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString(buildStatusString(), 15, MAP_H + 25);

        drawBottomMessage(g);
    }

    private void drawBottomMessage(Graphics g) {
        String msg = "WASD/Arrows to move | 1-9 to use item | '+' to toggle God mode";
        Color col = Color.LIGHT_GRAY;

        if (game.isWon()) {
            msg = "You reached the door! Press R to Restart.";
            col = Color.GREEN;
        } else if (game.getPlayer().getHealth() <= 0) {
            msg = "You died. Press R to Restart.";
            col = Color.RED;
        }

        g.setColor(col);
        g.drawString(msg, 15, MAP_H + 48);
    }

    private void drawCentered(Graphics g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (getWidth() - textWidth) / 2, y);
    }

    private void drawTextBlock(Graphics g, String[] lines, int startY, int step) {
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        int y = startY;
        for (int i = 0; i < lines.length; i++) {
            drawCentered(g, lines[i], y);
            y += step;
        }
    }

    private void renderMenu(Graphics g) {
        if (menuBg != null) {
            g.drawImage(menuBg, 0, 0, getWidth(), getHeight(), null);
        }
        int midY = getHeight() / 2;
        if (showCredits) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
            drawCentered(g, "CREDITS", midY - 100);
            drawTextBlock(g, CREDITS_TEXT, midY - 40, 30);
            drawCentered(g, "Press ESC or B to Back", midY + 100);
        } else {
            drawTextBlock(g, MENU_TEXT, midY + 20, 35);
        }
    }
}