package com.namiwin.salesgateway;

import android.telecom.Call;
import android.telecom.InCallService;

public class NamiInCallService extends InCallService {
    private static volatile Call current;

    @Override public void onCallAdded(Call call){ super.onCallAdded(call); current=call; }
    @Override public void onCallRemoved(Call call){ if(current==call)current=null; super.onCallRemoved(call); }

    public static boolean takeOver(){
        Call c=current; if(c==null)return false;
        try{
            if(c.getState()==Call.STATE_HOLDING)c.unhold();
            return true;
        }catch(Exception e){return false;}
    }

    public static boolean toggleHold(){
        Call c=current; if(c==null)return false;
        try{
            if(c.getState()==Call.STATE_HOLDING){c.unhold();return true;}
            Call.Details d=c.getDetails();
            if(d!=null && d.can(Call.Details.CAPABILITY_HOLD)){c.hold();return true;}
            return false;
        }catch(Exception e){return false;}
    }
}
