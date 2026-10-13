package com.nerodns.app;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.IOException;

public class DnsVpnService extends VpnService {
    private ParcelFileDescriptor vpnInterface = null;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";
        if ("STOP".equals(action)) {
            stopVpn();
            return START_NOT_STICKY;
        }
        try {
            Builder builder = new Builder();
            builder.setSession("Nero DNS");
            builder.addAddress("10.0.0.2", 32);
            builder.addDnsServer("82.22.175.205");
            builder.addDnsServer("1.1.1.1");
            vpnInterface = builder.establish();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }
    private void stopVpn() {
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (IOException ignored) {}
            vpnInterface = null;
        }
        stopSelf();
    }
    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
