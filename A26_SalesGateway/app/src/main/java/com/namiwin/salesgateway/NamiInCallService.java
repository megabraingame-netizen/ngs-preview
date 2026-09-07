package com.namiwin.salesgateway;

import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

public class NamiInCallService extends InCallService {
    private static volatile Call current;
    private static volatile NamiInCallService instance;

    @Override public void onCreate(){ super.onCreate(); instance=this; }
    @Override public void onDestroy(){ if(instance==this)instance=null; super.onDestroy(); }
    @Override public void onCallAdded(Call call){ super.onCallAdded(call); current=call; }
    @Override public void onCallRemoved(Call call){ if(current==call)current=null; super.onCallRemoved(call); }

    public static int currentState(){ Call c=current; return c==null?-1:c.getState(); }
    public static String stateName(int s){
        if(s==-1)return "NO_CALL";
        if(s==Call.STATE_NEW)return "NEW";
        if(s==Call.STATE_DIALING)return "DIALING";
        if(s==Call.STATE_RINGING)return "RINGING";
        if(s==Call.STATE_ACTIVE)return "ACTIVE";
        if(s==Call.STATE_HOLDING)return "HOLDING";
        if(s==Call.STATE_DISCONNECTED)return "DISCONNECTED";
        if(s==Call.STATE_CONNECTING)return "CONNECTING";
        if(s==Call.STATE_DISCONNECTING)return "DISCONNECTING";
        return String.valueOf(s);
    }

    public static boolean routeSpeaker(){
        NamiInCallService s=instance; if(s==null)return false;
        try{ s.setAudioRoute(CallAudioState.ROUTE_SPEAKER); return true; }catch(Exception e){return false;}
    }

    public static boolean takeOver(){
        Call c=current; if(c==null)return false;
        try{ if(c.getState()==Call.STATE_HOLDING)c.unhold(); return true; }catch(Exception e){return false;}
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
