/*
 * Copyright 2017-2026 Ralf Helbing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.pkmd.affinity.impl.LayoutEntities;

import de.pkmd.affinity.impl.GroupAffinityMask;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Objects;

/**
 * Created by rhelbing on 31.03.17.
 */
public class Core extends LayoutEntity {
	private Socket socket;

	public Core(GroupAffinityMask m) {
		super(m);
	}

	public Core(int group, long mask) {
		super(group, mask);
	}

	public Core(BitSet mask) {
		super(mask);
	}

	public Core(int id) {
		super(id);
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public Socket getSocket() {
		return socket;
	}

	public String toString() {
		return "C " + super.toString(); // + " on " + getSocket();
	}

	public String getLocation() {
		return getSocket().getLocation() + "/" + getId();
	}

	public int hashCode() {
		int socketId = -1;
		if (socket != null) {
			socketId = socket.getId();
		}
		return 1023 * socketId + getId();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Core otherCore = (Core) o;
		if (getId() != otherCore.getId()) {
			return false;
		}
		Socket otherSocket = otherCore.getSocket();
		if (!Objects.equals(otherSocket, getSocket())) {
			return false;
		}
		return true;

	}

	@Override
	public String getTypeName() {
		return "C";
	}

	@Override
	public ELayoutEntityType getEntityType() {
		return ELayoutEntityType.Core;
	}

}
