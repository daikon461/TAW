package dev.dualpower;

import java.util.UUID;

public final class ServerSwitchServiceSmokeTest {
    static final class Fake implements SafeSwitchCoordinator.Adapter {
        String power="fire";
        boolean fail;
        public Object snapshot(){return power;}
        public void validateSwitch(String a,String b){}
        public void clearEffects(String a){}
        public void assignPower(String to){power=to;}
        public void restoreSkills(String to){if(fail)throw new IllegalStateException("failed");}
        public void sync(){}
        public void rollback(Object snap){power=(String)snap;}
    }
    public static void main(String[] args) {
        ServerSwitchService service = new ServerSwitchService();
        UUID p=UUID.randomUUID();
        DualPowerState initial=new DualPowerState("fire","ice",0,0);
        service.load(p,initial);
        Fake a=new Fake(); a.fail=true;
        try { service.requestSwitch(p,100,a); throw new AssertionError(); }
        catch(IllegalStateException expected){}
        if(!initial.equals(service.get(p)) || !"fire".equals(a.power))
            throw new AssertionError("Failure must not commit state");
        a.fail=false;
        if(service.requestSwitch(p,100,a).activeSlot()!=1 || !"ice".equals(a.power))
            throw new AssertionError("Successful switch");
        try { service.requestSwitch(p,150,a); throw new AssertionError(); }
        catch(IllegalStateException expected){}
        service.unload(p);
        if(service.get(p)!=null)throw new AssertionError("Unload");
        System.out.println("PASS: commit, rollback, cooldown, unload");
    }
}
