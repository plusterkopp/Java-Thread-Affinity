package net.openhft.affinity.impl.LayoutEntities;

import net.openhft.affinity.impl.GroupAffinityMask;

import java.util.BitSet;

/**
 * Created by rhelbing on 31.03.17.
 */
public class Group extends LayoutEntity {

	protected Group(GroupAffinityMask m) {
		super(m);
	}

	public Group(int index, long mask) {
		super(index, mask);
	}

	public String getLocation() {
		return "" + getId();
	}

	public int getMSB() {
		BitSet bitSet = getBitMask();
		if (bitSet != null) {
			return bitSet.length();
		}
		GroupAffinityMask gam = getGroupMask();
		if (gam != null) {
			long mask = gam.getMask();
			return Long.SIZE - Long.numberOfLeadingZeros(mask);
		}
		return 0;
	}
}
