package com.reisi.lightcontrol.ui.device;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlService;

import org.jetbrains.annotations.NotNull;

public class DeviceActivityDebug <E extends LightControlService.LCSBinder> extends Fragment {
    private final static int TERMINAL_COMMAND = 0;
    private final static int TERMINAL_RESPONSE = 1;

    private final BluetoothDevice device;
    private final E serviceBinder;
    private final Context mContext;
    private TextView terminal;
    private final SpannableStringBuilder terminalHistory = new SpannableStringBuilder();

    public DeviceActivityDebug(Context context, BluetoothDevice device, E serviceBinder) {
        this.mContext = context;
        this.device = device;
        this.serviceBinder = serviceBinder;
    }

    private static IntentFilter makeIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(LightControlService.BROADCAST_UART);
        return filter;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String address = intent.getStringExtra(LightControlService.EXTRA_DEVICE_ADDRESS);
            if (address == null || !address.equals(device.getAddress()) || terminal == null) {
                return;
            }
            final String action = intent.getAction();
            if (LightControlService.BROADCAST_UART.equals(action)) {
                if (intent.hasExtra(LightControlService.EXTRA_UART_RX))
                    addTerminalText(intent.getStringExtra(LightControlService.EXTRA_UART_RX), TERMINAL_RESPONSE);
                // commands are added in the editor action listener, because the sent message has a too long delay
                /*if (intent.hasExtra(LightControlService.EXTRA_UART_TX)) {
                    addTerminalText(intent.getStringExtra(LightControlService.EXTRA_UART_RX), TERMINAL_COMMAND);
                }*/
            }
        }
    };

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void addTerminalText(String text, int type) {
        SpannableString newText;
        if (type == TERMINAL_COMMAND) {
            newText = new SpannableString("> " + text + "\r\n");
            newText.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.colorAccent)), 0, newText.length(), 0);
        }
        else if (type == TERMINAL_RESPONSE) {
            newText = new SpannableString(text);
            newText.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.colorSelected)), 0, newText.length(), 0);
        }
        else
            return;

        terminalHistory.append(newText);
        terminal.setText(terminalHistory, TextView.BufferType.SPANNABLE);
        final int scrollAmount = terminal.getLayout().getLineTop(terminal.getLineCount()) - terminal.getHeight();
        terminal.scrollTo(0, Math.max(scrollAmount, 0));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_device_debug, container, false);

        terminal = root.findViewById(R.id.terminal);
        terminal.setMovementMethod(new ScrollingMovementMethod());
        EditText command1 = root.findViewById(R.id.commandline);
        command1.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String command = v.getText().toString();
                serviceBinder.sendUartCommand(device, command);
                if (terminal != null)
                    addTerminalText(command, TERMINAL_COMMAND);
                return true;
            }
            return false;
        });

        LocalBroadcastManager.getInstance(mContext).registerReceiver(broadcastReceiver, makeIntentFilter());

        return root;
    }
}
