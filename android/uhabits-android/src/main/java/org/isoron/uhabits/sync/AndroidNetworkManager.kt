/*
 * Copyright (C) 2016-2020 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.sync

import android.content.*
import android.net.*
import org.isoron.uhabits.core.sync.*

class AndroidNetworkManager(
        val context: Context,
) : NetworkManager, ConnectivityManager.NetworkCallback() {

    val listeners = mutableListOf<NetworkManager.Listener>()
    var connected = false

    init {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(NetworkRequest.Builder().build(), this)
    }

    override fun addListener(listener: NetworkManager.Listener) {
        if (connected) listener.onNetworkAvailable()
        else listener.onNetworkLost()
        listeners.add(listener)
    }

    override fun remoteListener(listener: NetworkManager.Listener) {
        listeners.remove(listener)
    }

    override fun onAvailable(network: Network) {
        connected = true
        for (l in listeners) l.onNetworkAvailable()
    }

    override fun onLost(network: Network) {
        connected = false
        for (l in listeners) l.onNetworkLost()
    }
}