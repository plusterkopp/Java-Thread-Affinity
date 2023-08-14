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

    private String locationCache = null;

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
        if (locationCache != null) {
            return locationCache;
        }
        StringBuilder sb = new StringBuilder( 20);
        layout.cores.forEach( core -> {
            if ( servesCore( core)) {
                sb.append(core.getId()).append("+");
            }
        });
        if ( sb.length() > 0) {
            sb.setLength( sb.length() - 1);
        }
        locationCache = sb.toString();
        return locationCache;
    }

    private boolean servesCore(Core core) {
        GroupAffinityMask gmc = core.getGroupMask();
        GroupAffinityMask gm = getGroupMask();
        if ( gm.getGroupId() == gmc.getGroupId()) {
            long mc = gmc.getMask();
            long m = gm.getMask();
            if ( ( mc & m) != 0) {  // really, this should be a test that mc is a bitwise subset of m
                return true;
            }
        }
        return false;
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
                .filter( core -> core.groupAffinityMask.getGroupId() == groupAffinityMask.getGroupId())
                .filter( core -> ( groupAffinityMask.getMask() & core.groupAffinityMask.getMask()) != 0)
                .mapToInt( core -> core.getId())
                .toArray();
    }

    /**
     * check if size is larger than 1T, 1G, 1M or 1K. Divide by that unit size to obtain the number of T, G,
     * M, K or just B. Ignore any remainder.
     *
     * @param size
     * @param sb
     */
    public static void printSizeToSB( long size, StringBuilder sb) {
        long	sizesA[] = { 1L << 40, 1L << 30, 1L << 20, 1L << 10};
        char	suffixesA[] = { 'T', 'G', 'M', 'K'};

        for ( int i = 0;  i < sizesA.length;  i++) {
            long limit = sizesA[ i];
            if ( size > limit) {
                long multiple = size / limit;
                sb.append( multiple).append( suffixesA[ i]);
                return;
            }
        }
        sb.append( size).append( 'B');
    }

    public String toString() {
        StringBuilder sb = new StringBuilder( 100);
        sb.append( "L").append( level).append( getType().shortName()).append( " (");
        Cache.printSizeToSB( size, sb);
        sb.append( ") ").append( super.toString());
        return "" + level + " " + super.toString(); //  + " L" + level + " " + type.shortName();
    }
}
