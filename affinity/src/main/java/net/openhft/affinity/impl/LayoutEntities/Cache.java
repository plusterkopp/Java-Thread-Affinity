package net.openhft.affinity.impl.LayoutEntities;

import com.sun.jna.platform.win32.WinDef;
import net.openhft.affinity.CpuLayout;
import net.openhft.affinity.CacheCpuLayout;
import net.openhft.affinity.impl.GroupAffinityMask;
import net.openhft.affinity.impl.WindowsCpuLayout;

import java.util.ArrayList;
import java.util.List;

public class Cache extends LayoutEntity {

    static final byte FULL = (byte) 0xFF;
    private byte associativity;
    private CacheCpuLayout.CacheType type;
    private long size;
    private byte level;
    private short lineSize;
    CpuLayout   layout;

    public Cache(int group, long mask) {
        super(group, mask);
    }

    /**
     * must override so that L1 and L2 entries don't mask each other
     * @param o
     * @return
     */
    public int compareTo(LayoutEntity o) {
        if ( o instanceof Cache) {
            Cache c = (Cache) o;
            int levelComp = Integer.compare( level, c.level);
            if ( levelComp != 0) {
                return levelComp;
            }
        }
        return super.compareTo( o);
    }

    public void setType(byte t) {
        type = CacheCpuLayout.CacheType.getInstance( t);
    }

    public void setAssociativity(byte a) {
        associativity = a;
    }

    public void setSize(WinDef.DWORD cacheSize) {
        size = cacheSize.longValue();
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public byte getLevel() {
        return level;
    }

    public void setLineSize(WinDef.WORD ls) {
        lineSize = ls.shortValue();
    }

    @Override
    public String getLocation() {
        if ( layout instanceof WindowsCpuLayout) {
            return getLocation( ( WindowsCpuLayout) layout);
        }
        return "" + groupAffinityMask;
    }

    private String getLocation( WindowsCpuLayout layout) {
        StringBuilder sb = new StringBuilder( 20);
        layout.cores.forEach( core -> sb.append( core.getId()).append( "+"));
        if ( sb.length() > 0) {
            sb.setLength( sb.length() - 1);
        }
        return sb.toString();
    }

    public void setLayout( CpuLayout cpuLayout) {
        layout = cpuLayout;
    }

    public long getSize() {
        return size;
    }

    public long getLineSize() {
        return lineSize;
    }

    public byte getAssociativity() {
        return associativity;
    }

    public CacheCpuLayout.CacheType getType() {
        return type;
    }

    public int[] getCores() {
        if ( ! ( layout instanceof WindowsCpuLayout)) {
            return new int[ 0];
        }
        WindowsCpuLayout wLayout = (WindowsCpuLayout) layout;
        return wLayout.cores.stream()
                .filter( core -> core.groupAffinityMask.getGroupId() != groupAffinityMask.getGroupId())
                .filter( core -> ( groupAffinityMask.getMask() & core.groupAffinityMask.getMask()) != 0)
                .mapToInt( core -> core.getId())
                .toArray();
    }

    public String toString() {
        return super.toString() + " L" + level + " " + type.shortName();
    }
}
