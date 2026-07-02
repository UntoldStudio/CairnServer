package org.cairnserver.world;

public record Location(World world, double x, double y, double z, float yaw, float pitch) {
    public Location {
        yaw %= 360;
        pitch %= 180;
    }

    public Location(World world, double x, double y, double z) {
        this(world, x, y, z, 0f, 0f);
    }

    public BlockPosition getBlockPosition() {
        return new BlockPosition(getBlockX(), getBlockY(), getBlockZ());
    }

    public Location add(double dx, double dy, double dz, float yaw, float pitch) {
        return new Location(world, this.x + dx,  this.y + dy, this.z + dz, this.yaw + yaw, this.pitch + pitch);
    }

    public Location add(double dx, double dy, double dz) {
        return this.add(dx, dy, dz, 0f, 0f);
    }

    public Location sub(double dx, double dy, double dz, float yaw, float pitch) {
        return new Location(world, this.x - dx, this.y - dy, this.z - dz, this.yaw - yaw, this.pitch - pitch);
    }

    public Location sub(double dx, double dy, double dz) {
        return this.sub(dx, dy, dz, 0f, 0f);
    }

    public int getBlockX() {
        return (int) x;
    }

    public int getBlockY() {
        return (int) y;
    }

    public int getBlockZ() {
        return (int) z;
    }
}
