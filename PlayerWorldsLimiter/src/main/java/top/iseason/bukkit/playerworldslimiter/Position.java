package top.iseason.bukkit.playerworldslimiter;

import lombok.Getter;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.List;


@Getter
public class Position {
    private final int x;
    private final int y;
    private final int z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }

    public boolean check(Block block) {
        return x == block.getX() && y == block.getY() && z == block.getZ();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Position)) return false;
        Position pos = (Position) obj;
        return x == pos.getX() && y == pos.getY() && z == pos.getZ();
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    public static Position fromString(String str) {
        String[] split = str.split(",");
        if (split.length != 3) return null;
        try {
            return new Position(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        } catch (Exception e) {
            return null;
        }
    }

    public static HashSet<Position> fromStringList(List<String> list) {
        HashSet<Position> positions = new HashSet<>();
        for (String s : list) {
            Position position = Position.fromString(s);
            if (position != null)
                positions.add(position);
        }
        return positions;
    }

    public static Position fromBlock(Block block) {
        return new Position(block.getX(), block.getY(), block.getZ());
    }
}
