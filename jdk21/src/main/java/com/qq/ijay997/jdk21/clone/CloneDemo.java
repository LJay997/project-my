package com.qq.ijay997.jdk21.clone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Java 对象 clone 的完整演示 —— 覆盖浅拷贝、深拷贝、拷贝构造器、
 * 序列化、不可变对象、Record 等主流方案。
 *
 * <h3>运行方式</h3>
 * <pre>
 * cd jdk21
 * mvn compile -q
 * java -cp target/classes com.qq.ijay997.jdk21.clone.CloneDemo
 * </pre>
 *
 * <h3>六种方案选择指南</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>方案</th><th>适用场景</th><th>风险</th></tr>
 *   <tr><td>{@code Object.clone()} 浅拷贝</td><td>字段全部是基本类型或不可变引用</td><td>字段类型一变就出 Bug</td></tr>
 *   <tr><td>{@code Object.clone()} 重写做深拷贝</td><td>字段不多且不想加额外依赖</td><td>每个可变字段都要手动 clone，漏一个就变成"假深拷贝"</td></tr>
 *   <tr><td>拷贝构造器</td><td>任何场景（Effective Java 首选）</td><td>需要手动写一个构造器</td></tr>
 *   <tr><td>序列化 / 反序列化</td><td>有循环引用或对象图复杂</td><td>所有关联类都要 Serializable，性能最差</td></tr>
 *   <tr><td>不可变对象 + withXxx</td><td>业务模型本身就是不可变值对象</td><td>需要先改造成不可变</td></tr>
 *   <tr><td>第三方库（BeanUtils / ModelMapper）</td><td>字段多、Dozer 风格转换</td><td>反射开销大、部分库有深拷贝陷阱</td></tr>
 * </table>
 */
public class CloneDemo {

    public static void main(String[] args) throws Exception {
        header("Java 对象 Clone 完整演示");
        demoShallowClone();
        demoShallowClonePitfall();
        demoDeepCloneOverride();
        demoCopyConstructor();
        demoSerializationClone();
        demoImmutableWithXxx();
        demoRecordWithXxx();
        footer();
    }

    // ── 0. 准备数据 ────────────────────────────────────────────────

    private static Person buildPerson() {
        return new Person(
                "张三", 30,
                new Address("朝阳路 88 号", "北京"),
                new ArrayList<>(List.of("读书", "跑步"))
        );
    }

    private static DeepCopyPerson buildDeepCopyPerson() {
        return new DeepCopyPerson(
                "李四", 28,
                new Address("中山大道 1 号", "广州"),
                new ArrayList<>(List.of("篮球", "美食"))
        );
    }

    // ── 1. 浅拷贝：Object.clone 的默认行为 ─────────────────────────

    /**
     * 浅拷贝：基本类型和不可变引用被正确复制，但 <b>可变引用只是复制了值</b>。
     * 所以克隆对象的 address / hobbies 和原对象指向同一个实例。
     */
    static void demoShallowClone() {
        section("① 浅拷贝（Object.clone 默认行为）");

        Person original = buildPerson();
        Person clone = original.clone();

        print("原对象", original);
        print("克隆对象", clone);
        note("两者字段值相同，但 address / hobbies 是同一个引用 ↓");

        // 证明：改 clone 的 address，original 也被修改
        clone.getAddress().setCity("上海");
        clone.getHobbies().add("打游戏");

        note("clone 改了 address / hobbies 之后：");
        print("原对象", original);
        print("克隆对象", clone);
        warn("原对象的 address.city 也变成了 '上海'，hobbies 里也多了 '打游戏' —— 浅拷贝的经典陷阱！");
        separator();
    }

    // ── 2. 浅拷贝的坑演示 ──────────────────────────────────────────

    static void demoShallowClonePitfall() {
        section("② 浅拷贝的坑：修改克隆对象污染原对象");

        Person p1 = buildPerson();
        Person p2 = p1.clone();

        // 验证同一个引用
        boolean sameAddress = p1.getAddress() == p2.getAddress();
        boolean sameHobbies = p1.getHobbies() == p2.getHobbies();
        info("p1.address == p2.address  ? " + sameAddress + "  " + (sameAddress ? "← 同一个实例！" : "✓ 独立"));
        info("p1.hobbies == p2.hobbies ? " + sameHobbies + "  " + (sameHobbies ? "← 同一个实例！" : "✓ 独立"));
        info("p1.name == p2.name        ? " + (p1.getName() == p2.getName()) + "  (String 不可变，== 比较字符串常量池，不一定能说明问题，看字段值即可)");

        // 改 clone 的基本类型 / String → 原对象不受影响
        p2.setName("另一个张三");
        p2.setAge(99);
        print("改 clone 的基本类型/字符串后，原对象", p1);
        print("克隆对象", p2);
        info("基本类型和不可变引用的修改是隔离的，只有可变引用字段才共享");
        separator();
    }

    // ── 3. 深拷贝：重写 clone() ─────────────────────────────────────

    static void demoDeepCloneOverride() {
        section("③ 深拷贝（重写 clone()，逐字段重建）");

        DeepCopyPerson original = buildDeepCopyPerson();
        DeepCopyPerson clone = original.clone();

        // 改 clone 的 address 和 hobbies
        clone.getAddress().setCity("深圳");
        clone.getHobbies().add("摄影");

        print("原对象（未受影响）", original);
        print("克隆对象", clone);

        // 验证是独立引用
        boolean independentAddress = original.getAddress() != clone.getAddress();
        boolean independentHobbies = original.getHobbies() != clone.getHobbies();
        info("address 独立: " + independentAddress + "  hobbies 独立: " + independentHobbies);
        ok("深拷贝生效！");
        separator();
    }

    // ── 4. 拷贝构造器 ──────────────────────────────────────────────

    /**
     * Effective Java 推荐：比重写 clone() 更灵活、更安全、更易读。
     * 可以接受子类，不需要 implements Cloneable，不会抛 checked 异常。
     */
    static void demoCopyConstructor() {
        section("④ 拷贝构造器（Effective Java 首选）");

        DeepCopyPerson original = buildDeepCopyPerson();
        // 拷贝构造器：DeepCopyPerson(DeepCopyPerson other)
        DeepCopyPerson clone = new DeepCopyPerson(original);

        clone.getAddress().setCity("杭州");
        clone.getHobbies().add("登山");

        print("原对象（未受影响）", original);
        print("克隆对象（通过拷贝构造器）", clone);
        boolean independent = original.getAddress() != clone.getAddress()
                && original.getHobbies() != clone.getHobbies();
        ok("拷贝构造器深拷贝生效：" + independent);
        separator();
    }

    // ── 5. 序列化深拷贝 ────────────────────────────────────────────

    /**
     * 把对象写成字节流再读回来。适合有循环引用、对象图复杂的场景。
     * 代价：所有关联类都要 {@link java.io.Serializable}，性能最差（字节流编解码）。
     */
    static void demoSerializationClone() throws Exception {
        section("⑤ 序列化深拷贝");

        Person original = buildPerson();
        Person clone = serializationClone(original);

        clone.getAddress().setCity("成都");
        clone.getHobbies().add("麻将");

        print("原对象（未受影响）", original);
        print("克隆对象（序列化回来的全新实例）", clone);
        boolean independent = original.getAddress() != clone.getAddress()
                && original.getHobbies() != clone.getHobbies();
        ok("序列化深拷贝生效：" + independent);
        separator();
    }

    /** 序列化深拷贝工具方法：通用、递归处理整个对象图 */
    @SuppressWarnings("unchecked")
    static <T> T serializationClone(T obj) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    // ── 6. 不可变对象 + withXxx 模式 ────────────────────────────────

    /**
     * 不可变对象不需要 clone。
     * 修改时返回新实例，旧实例原封不动 —— 天然没有浅拷贝污染问题。
     */
    static void demoImmutableWithXxx() {
        section("⑥ 不可变对象 + withXxx（不用 clone）");

        Employee original = new Employee("E-1001", "王五", 10_000,
                new ArrayList<>(List.of("Java", "SQL")));

        // withSalary 返回新实例，original 完全不受影响
        Employee promoted = original.withSalary(15_000);
        Employee withSkill = promoted.addSkill("Kubernetes");

        print("original", original);
        print("promoted (withSalary)", promoted);
        print("withSkill (promoted.addSkill)", withSkill);

        // original 的 skills 不会被外部修改（List.copyOf 防御性拷贝 → 不可变列表）
        try {
            original.getSkills().add("Python");
            warn("不应该走到这里！防御性拷贝失效了");
        } catch (UnsupportedOperationException e) {
            info("original.getSkills() 返回不可变列表，外部 add 抛 UnsupportedOperationException —— 防御性拷贝生效");
        }
        ok("不可变对象：原始值安全、修改得到新值、没有浅拷贝陷阱");
        separator();
    }

    // ── 7. Java Record + withXxx ───────────────────────────────────

    /**
     * Record (JDK 16+) 是不可变值对象的首选语法糖。
     * 组件默认 final，equals/hashCode/toString 自动生成，天然适合做拷贝场景。
     */
    static void demoRecordWithXxx() {
        section("⑦ Java Record + withXxx（JDK 16+ 语法糖）");

        ImmutablePerson original = new ImmutablePerson(
                "赵六", 25,
                new ImmutablePerson.ImmutableAddress("长安街", "北京"),
                new ArrayList<>(List.of("摄影")));

        // withAddress 返回新 Record 实例
        ImmutablePerson moved = original.withAddress(
                original.address().withCity("上海"));

        ImmutablePerson withMore = moved.addHobby("滑板");

        print("original", original);
        print("moved (withAddress.withCity)", moved);
        print("withMore (addHobby)", withMore);

        // Record 的组件字段天然独立：original.hobbies 已经是不可变 List
        info("Record hobbies 通过紧凑构造器的 List.copyOf 做了防御性拷贝，外部无法修改");
        ok("Record + withXxx：最简洁的不可变拷贝方案");
        separator();
    }

    // ── 辅助输出 ───────────────────────────────────────────────────

    private static void header(String title) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  " + title);
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    private static void footer() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  经验总结                                                  ║");
        System.out.println("║  1) 可变对象：拷贝构造器或序列化（比重写 clone() 更可靠）    ║");
        System.out.println("║  2) 不可变对象：withXxx 或 copy() 工厂方法                  ║");
        System.out.println("║  3) 新模型优先 Record，其次手写 final + 防御性拷贝          ║");
        System.out.println("║  4) 深拷贝不要半拉子：漏一个字段就是假深拷贝                 ║");
        System.out.println("║  5) 循环引用用序列化或 Graph 库，别自己递归                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("──── " + title + " ────");
    }

    private static void separator() {
        System.out.println();
    }

    private static void print(String label, Object o) {
        System.out.println("  " + label + ": " + o);
    }

    private static void note(String s)  { System.out.println("  📝 " + s); }
    private static void info(String s)  { System.out.println("  ℹ️  " + s); }
    private static void ok(String s)    { System.out.println("  ✅ " + s); }
    private static void warn(String s)  { System.out.println("  ⚠️  " + s); }
}
