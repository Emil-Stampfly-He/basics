package proxy.cglib;

import net.sf.cglib.core.Signature;

public class ProxyFastClass {

    static Signature signature0 = new Signature("saveOriginal", "()V");
    static Signature signature1 = new Signature("saveOriginal", "(I)V");
    static Signature signature2 = new Signature("saveOriginal", "(J)V");

    /**
     * 获取代理方法的编号
     * Proxy
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

    public Object invoke(int index, Object proxy, Object[] args) {
        return switch (index) {
            case 0 -> {
                (($Proxy0) proxy).saveOrigin();
                yield null;
            }
            case 1 -> {
                (($Proxy0) proxy).saveOrigin((int) args[0]);
                yield null;
            }
            case 2 -> {
                (($Proxy0) proxy).saveOrigin((long) args[0]);
                yield null;
            }
            default -> throw new NoSuchMethodError("No such method!");
        };
    }

    public static void main(String[] args) {
        ProxyFastClass proxyFastClass = new ProxyFastClass();
        int index = proxyFastClass.getIndex(new Signature("saveOriginal", "()V"));
        proxyFastClass.invoke(index, new $Proxy0(), new Object[0]); // save()

        index = proxyFastClass.getIndex(new Signature("saveOriginal", "(I)V"));
        proxyFastClass.invoke(index, new $Proxy0(), new Object[]{100}); // save(int)

    }
}

