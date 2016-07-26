package kr.ac.kaist.gan.zonedetector;

/**
 * Created by iDB_ADB on 2016-07-19.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOBeaconRegionState;

import java.util.ArrayList;
import java.util.HashMap;

public class RecoMonitoringListAdapter extends BaseAdapter {

    private HashMap<RECOBeaconRegion, RECOBeaconRegionState> mMonitoredRegions;
    private HashMap<RECOBeaconRegion, String> mLastUpdateTime;
    private HashMap<RECOBeaconRegion, Integer> mMatchedBeaconCounts;
    private ArrayList<RECOBeaconRegion> mMonitoredRegionLists;

    private LayoutInflater mLayoutInflater;

    public RecoMonitoringListAdapter(Context context) {
        super();
        mMonitoredRegions = new HashMap<RECOBeaconRegion, RECOBeaconRegionState>();
        mLastUpdateTime = new HashMap<RECOBeaconRegion, String>();
        mMatchedBeaconCounts = new HashMap<RECOBeaconRegion, Integer>();
        mMonitoredRegionLists = new ArrayList<RECOBeaconRegion>();

        mLayoutInflater = LayoutInflater.from(context);
    }

    public void updateRegion(RECOBeaconRegion recoRegion, RECOBeaconRegionState recoState, int beaconCount, String updateTime) {
        mMonitoredRegions.put(recoRegion, recoState);
        mLastUpdateTime.put(recoRegion, updateTime);
        mMatchedBeaconCounts.put(recoRegion, beaconCount);
        if(!mMonitoredRegionLists.contains(recoRegion)) {
            mMonitoredRegionLists.add(recoRegion);
        }
    }

    public void clear() {
        mMonitoredRegions.clear();
    }

    @Override
    public int getCount() {
        return mMonitoredRegions.size();
    }

    @Override
    public Object getItem(int position) {
        return mMonitoredRegions.get(mMonitoredRegionLists.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_monitoring_region, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.recoRegionID = (TextView)convertView.findViewById(R.id.region_uniqueID);
            viewHolder.recoRegionState = (TextView)convertView.findViewById(R.id.region_state);
            viewHolder.recoRegionTime = (TextView)convertView.findViewById(R.id.region_update_time);
            viewHolder.recoRegionBeaconCount = (TextView)convertView.findViewById(R.id.region_beacon_count);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        RECOBeaconRegion recoRegion = mMonitoredRegionLists.get(position);
        RECOBeaconRegionState recoState = mMonitoredRegions.get(recoRegion);

        String recoRegionUniqueID = recoRegion.getUniqueIdentifier();
        String recoRegionState = recoState.toString();
        String recoUpdateTime = mLastUpdateTime.get(recoRegion);
        String recoBeaconCount = mMatchedBeaconCounts.get(recoRegion).toString();

        viewHolder.recoRegionID.setText(recoRegionUniqueID);
        viewHolder.recoRegionState.setText(recoRegionState);
        viewHolder.recoRegionTime.setText(recoUpdateTime);

        if(recoRegionState.equals(RECOBeaconRegionState.RECOBeaconRegionInside.toString()) && mMatchedBeaconCounts.get(recoRegion) == 0) {
            viewHolder.recoRegionBeaconCount.setText("You started monitoring inside of the region.");
            return convertView;
        }

        if(recoRegionState.equals(RECOBeaconRegionState.RECOBeaconRegionOutside.toString())) {
            viewHolder.recoRegionBeaconCount.setText("No beacons around.");
            return convertView;
        }

        viewHolder.recoRegionBeaconCount.setText("# of beacons in the region: " + recoBeaconCount);

        return convertView;
    }

    static class ViewHolder {
        TextView recoRegionID;
        TextView recoRegionState;
        TextView recoRegionTime;
        TextView recoRegionBeaconCount;
    }

}
