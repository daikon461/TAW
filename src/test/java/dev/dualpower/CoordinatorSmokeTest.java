package dev.dualpower;

public class CoordinatorSmokeTest {
    static class Fake implements SafeSwitchCoordinator.Adapter {
        String power="fire"; boolean fail=false; boolean restored=false;
        public Object snapshot(){return power;}
        public void validateSwitch(String from,String to){}
        public void clearEffects(String from){}
        public void assignPower(String to){power=to;}
        public void restoreSkills(String to){if(fail) throw new IllegalStateException("simulated failure");}
        public void sync(){}
        public void rollback(Object snapshot){power=(String)snapshot; restored=true;}
    }
    public static void main(String[] args) {
        DualPowerState s=new DualPowerState("fire","ice",0,0);
        Fake ok=new Fake();
        DualPowerState next=SafeSwitchCoordinator.switchPower(s,100,ok);
        if (!"ice".equals(ok.power) || next.activeSlot()!=1) throw new AssertionError("switch");
        if(next.canSwitch(199,100)) throw new AssertionError("cooldown");
        Fake bad=new Fake(); bad.fail=true;
        try { SafeSwitchCoordinator.switchPower(s,100,bad); throw new AssertionError("expected failure"); }
        catch(IllegalStateException expected) {}
        if (!bad.restored || !"fire".equals(bad.power)) throw new AssertionError("rollback");
        System.out.println("PASS: switch, cooldown, rollback");
    }
}
