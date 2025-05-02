package proxy.cglib;

import net.sf.cglib.core.Signature;

public class TargetFastClass {

    static Signature signature0 = new Signature("save", "()V");
    static Signature signature1 = new Signature("save", "(I)V");
    static Signature signature2 = new Signature("save", "(J)V");

    /**
     * 获取目标方法的编号
     * Target
     *     save()        0
     *     save(int)     1
     *     save(long)    2
     * @param signature 方法签名，包含方法名，方法参数和方法返回类型
     * @return 方法编号
     */
    public int getIndex(Signature signature){
        if (signature.equals(signature0)){
            return 0;
        } else if (signature.equals(signature1)) {
            return 1;
        }  else if (signature.equals(signature2)) {
            return 2;
        } else {
            return -1;
        }
    }

    public Object invoke(int index, Object target, Object[] args) {
        return switch (index) {
            case 0 -> {
                ((Target) target).save();
                yield null;
            }
            case 1 -> {
                ((Target) target).save((int) args[0]);
                yield null;
            }
            case 2 -> {
                ((Target) target).save((long) args[0]);
                yield null;
            }
            default -> throw new NoSuchMethodError("No such method!");
        };
    }
}
