package proxy.cglib;

import net.sf.cglib.core.Signature;

public class TestTargetFastClass {
    public static void main(String[] args) {
        TargetFastClass targetFastClass = new TargetFastClass();
        int index = targetFastClass.getIndex(new Signature("save", "()V"));
        System.out.println(index);

        targetFastClass.invoke(index, new Target(), new Object[0]); // save()

        index = targetFastClass.getIndex(new Signature("save", "(I)V"));
        targetFastClass.invoke(index, new Target(), new Object[]{1}); // save(int)
    }
}
