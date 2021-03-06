package com.tcl.alvin.tcl_drone_project.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.tcl.alvin.tcl_drone_project.R;
import com.tcl.alvin.tcl_drone_project.controller.TCLDroneDiscoverer;
import com.tcl.alvin.tcl_drone_project.util.TCLNdkJniUtils;

import java.util.ArrayList;
import java.util.List;

public class TCLDeviceListActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_SERVICE = "EXTRA_DEVICE_SERVICE";

    private static final String TAG = "TCLDeviceListActivity";

    public TCLDroneDiscoverer mDroneDiscoverer;

    private final List<ARDiscoveryDeviceService> mDronesList = new ArrayList<>();

    // this block loads the native libraries
    // it is mandatory
    static {
        ARSDK.loadSDKLibs();
        System.loadLibrary("TCLJni");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        final ListView listView = (ListView) findViewById(R.id.list);
        System.out.println("[TCL DEBUG]:"+ TCLNdkJniUtils.getStringFormC());
        System.out.println("[TCL DEBUG]:AAAAAAAAAAAAAAAAAAAAA");
        // Assign adapter to ListView
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                // launch the activity related to the type of discovery device service
                Intent intent = null;

                ARDiscoveryDeviceService service = (ARDiscoveryDeviceService)mAdapter.getItem(position);
                ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());
                switch (product) {
                    case ARDISCOVERY_PRODUCT_ARDRONE:
                    case ARDISCOVERY_PRODUCT_BEBOP_2:
                        intent = new Intent(TCLDeviceListActivity.this, TCLBebopActivity.class);
                        break;

                    default:
                        Log.e(TAG, "The type " + product + " is not supported by this sample");
                }

                if (intent != null) {
                    intent.putExtra(EXTRA_DEVICE_SERVICE, service);
                    startActivity(intent);
                }
            }
        });

        mDroneDiscoverer = new TCLDroneDiscoverer(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // setup the drone discoverer and register as listener
        mDroneDiscoverer.setup();
        mDroneDiscoverer.addListener(mDiscovererListener);

        // start discovering
        mDroneDiscoverer.startDiscovering();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // clean the drone discoverer object
        mDroneDiscoverer.stopDiscovering();
        mDroneDiscoverer.cleanup();
        mDroneDiscoverer.removeListener(mDiscovererListener);
    }

    private final TCLDroneDiscoverer.Listener mDiscovererListener = new  TCLDroneDiscoverer.Listener() {

        @Override
        public void onDronesListUpdated(List<ARDiscoveryDeviceService> dronesList) {
            mDronesList.clear();
            mDronesList.addAll(dronesList);

            mAdapter.notifyDataSetChanged();
        }
    };

    static class ViewHolder {
        public TextView text;
    }

    private final BaseAdapter mAdapter = new BaseAdapter()
    {
        @Override
        public int getCount()
        {
            return mDronesList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mDronesList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                LayoutInflater inflater = getLayoutInflater();
                rowView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                // configure view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.text = (TextView) rowView.findViewById(android.R.id.text1);
                rowView.setTag(viewHolder);
            }

            // fill data
            ViewHolder holder = (ViewHolder) rowView.getTag();
            ARDiscoveryDeviceService service = (ARDiscoveryDeviceService)getItem(position);
            holder.text.setText(service.getName());

            return rowView;
        }
    };
}
