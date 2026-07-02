package org.cairnserver.world;

public record BlockPosition(int x, int y, int z) {
    public BlockPosition add(int dx, int dy, int dz) {
        return new BlockPosition(this.x + dx, this.y + dy, this.z + dz);
    }
    public BlockPosition sub(int dx, int dy, int dz) {
        return new BlockPosition(this.x - dx, this.y - dy, this.z - dz);
    }
}