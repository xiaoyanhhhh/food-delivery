package com.fooddelivery.config;

import com.fooddelivery.entity.*;
import com.fooddelivery.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.fooddelivery.repository.DishSpecificationRepository specRepository;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("zhangsan").isPresent()) {
            ensureRiderTypes();
            log.info("数据库已有测试数据，跳过初始化");
            return;
        }
        log.info("数据库为空，开始初始化测试数据...");
        log.info("========================================");
        log.info("   开始初始化美团外卖数据...");
        log.info("========================================");

        // ========== 1. 创建顾客账号 ==========
        User customer = createUser("zhangsan", "123456", "13800138001", User.Role.CUSTOMER, "北京市朝阳区望京SOHO T1");
        log.info("✅ 顾客: zhangsan / 123456");

        // ========== 2. 创建店铺分类 ==========
        StoreCategory chinese = createStoreCategory("中餐", "🍚", 1);
        StoreCategory fastFood = createStoreCategory("快餐", "🍔", 2);
        StoreCategory snack = createStoreCategory("小吃", "🍢", 3);
        StoreCategory drink = createStoreCategory("饮品", "🧋", 4);
        StoreCategory dessert = createStoreCategory("甜品", "🍰", 5);
        StoreCategory western = createStoreCategory("西餐", "🍕", 6);
        log.info("✅ 店铺分类: 中餐/快餐/小吃/饮品/甜品/西餐");

        // ========== 3. 创建商家账号 + 店铺 ==========

        // 商家1: 川香阁
        User m1 = createUser("chuanxiangge", "123456", "13900000001", User.Role.MERCHANT, "北京市东城区王府井大街100号");
        Store s1 = createStore("川香阁", "https://img.alicdn.com/imgextra/i3/2200721622027/O1CN01UjJmZx1mYKHtRkZqN_!!2200721622027.jpg",
                "正宗川菜，麻辣鲜香", chinese, m1, "09:00-22:30", "新店开业，全场8折！", "20.00", "北京市东城区王府井大街100号");

        // 商家2: 麦肯基汉堡
        User m2 = createUser("maikenji", "123456", "13900000002", User.Role.MERCHANT, "北京市海淀区中关村大街1号");
        Store s2 = createStore("麦肯基汉堡", "",
                "现做汉堡，新鲜薯条", fastFood, m2, "07:00-23:00", "周三会员日，买一送一", "15.00", "北京市海淀区中关村大街1号");

        // 商家3: 老北京炸酱面
        User m3 = createUser("laobeijing", "123456", "13900000003", User.Role.MERCHANT, "北京市西城区西单北大街200号");
        Store s3 = createStore("老北京炸酱面馆", "",
                "地道京味，传承百年", chinese, m3, "06:30-21:00", "老北京味道，每日新鲜手擀面", "18.00", "北京市西城区西单北大街200号");

        // 商家4: 深海寿司
        User m4 = createUser("shenhaishousi", "123456", "13900000004", User.Role.MERCHANT, "北京市朝阳区三里屯太古里B1");
        Store s4 = createStore("深海寿司", "",
                "新鲜刺身，精致日料", western, m4, "11:00-22:00", "空运食材，每日限量供应", "30.00", "北京市朝阳区三里屯太古里B1");

        // 商家5: 茶颜悦饮
        User m5 = createUser("chayan", "123456", "13900000005", User.Role.MERCHANT, "北京市朝阳区国贸商城3F");
        Store s5 = createStore("茶颜悦饮", "",
                "现泡好茶，鲜果茶饮", drink, m5, "09:00-22:00", "第二杯半价", "10.00", "北京市朝阳区国贸商城3F");

        // 商家6: 甜心烘焙
        User m6 = createUser("tianxin", "123456", "13900000006", User.Role.MERCHANT, "北京市海淀区五道口购物中心");
        Store s6 = createStore("甜心烘焙", "",
                "手工蛋糕，法式甜品", dessert, m6, "08:00-21:30", "新鲜出炉，每日现做", "25.00", "北京市海淀区五道口购物中心");

        log.info("✅ 6家特色店铺创建完成");

        // ========== 4. 创建菜品分类 ==========
        Category c1 = createCategory("招牌菜", 1);
        Category c2 = createCategory("主食", 2);
        Category c3 = createCategory("汤品", 3);
        Category c4 = createCategory("饮品", 4);
        Category c5 = createCategory("小食", 5);
        Category c6 = createCategory("套餐", 6);
        Category c7 = createCategory("甜品", 7);
        log.info("✅ 菜品分类创建完成");

        // ========== 5. 各店铺发布菜品 ==========

        // === 川香阁 ===
        createDish("宫保鸡丁", "精选鸡腿肉，配以花生、干辣椒爆炒，甜辣适中、回味无穷", new BigDecimal("38.00"),
                "https://img.alicdn.com/imgextra/i2/2200721622027/O1CN01XZfJqD1mYKHnJNnpP_!!2200721622027.jpg", c1, s1, 1520);
        createDish("水煮鱼", "鲜活草鱼片，配以豆芽、干辣椒、花椒，麻辣烫鲜", new BigDecimal("68.00"), "", c1, s1, 980);
        createDish("麻婆豆腐", "嫩豆腐配以牛肉末，麻辣烫香，下饭神器", new BigDecimal("22.00"), "", c1, s1, 2100);
        createDish("回锅肉", "五花肉配蒜苗，郫县豆瓣酱炒制，肥而不腻", new BigDecimal("35.00"), "", c1, s1, 1350);
        createDish("担担面", "正宗四川担担面，芝麻酱、肉末、花生碎，香气扑鼻", new BigDecimal("18.00"), "", c2, s1, 1800);
        createDish("酸辣粉", "红薯粉配以酸辣汤汁、花生、香菜，开胃爽口", new BigDecimal("15.00"), "", c2, s1, 2200);
        createDish("番茄蛋花汤", "新鲜番茄配鸡蛋，清淡鲜美", new BigDecimal("12.00"), "", c3, s1, 890);
        createDish("冰粉", "手工冰粉配红糖、花生、葡萄干，清凉解暑", new BigDecimal("10.00"), "", c7, s1, 760);

        // === 麦肯基汉堡 ===
        createDish("香辣鸡腿堡", "超大鸡腿肉，裹以香辣脆皮，配新鲜生菜和秘制酱料", new BigDecimal("25.00"), "", c1, s2, 3200);
        createDish("双层牛肉堡", "双片澳洲牛肉饼，配车达芝士、酸黄瓜、番茄", new BigDecimal("35.00"), "", c1, s2, 2800);
        createDish("黄金薯条", "粗切土豆条，外酥里嫩，配番茄酱", new BigDecimal("12.00"), "", c5, s2, 5600);
        createDish("香辣鸡翅", "秘制腌料腌制24小时，外酥里嫩", new BigDecimal("15.00"), "", c5, s2, 4100);
        createDish("可乐", "冰镇可口可乐", new BigDecimal("8.00"), "", c4, s2, 6800);
        createDish("超值套餐A", "香辣鸡腿堡+薯条+可乐", new BigDecimal("39.00"), "", c6, s2, 1900);

        // === 老北京炸酱面 ===
        createDish("老北京炸酱面", "手擀面配以黄酱炒制的肉丁炸酱，配黄瓜丝、豆芽", new BigDecimal("22.00"), "", c1, s3, 3100);
        createDish("打卤面", "宽面条配以木耳、黄花菜、鸡蛋卤子，鲜美浓郁", new BigDecimal("20.00"), "", c2, s3, 1450);
        createDish("北京烤鸭卷", "薄饼卷烤鸭片，配葱丝、甜面酱", new BigDecimal("45.00"), "", c1, s3, 870);
        createDish("京酱肉丝", "猪里脊丝配甜面酱，配豆腐皮和葱丝", new BigDecimal("32.00"), "", c1, s3, 1200);
        createDish("炸酱拌面套餐", "炸酱面+京酱肉丝+酸梅汤", new BigDecimal("48.00"), "", c6, s3, 650);
        createDish("酸梅汤", "老北京传统酸梅汤，冰镇更佳", new BigDecimal("8.00"), "", c4, s3, 1800);

        // === 深海寿司 ===
        createDish("三文鱼刺身", "挪威进口三文鱼，厚切8片，配芥末和酱油", new BigDecimal("88.00"), "", c1, s4, 720);
        createDish("金枪鱼刺身", "蓝鳍金枪鱼中腹，入口即化", new BigDecimal("128.00"), "", c1, s4, 380);
        createDish("鳗鱼寿司", "蒲烧鳗鱼手握寿司，2贯", new BigDecimal("35.00"), "", c1, s4, 950);
        createDish("加州卷", "蟹肉棒、牛油果、黄瓜，配飞鱼籽", new BigDecimal("28.00"), "", c1, s4, 1100);
        createDish("日式拉面", "猪骨浓汤拉面，配叉烧、溏心蛋、海苔", new BigDecimal("38.00"), "", c2, s4, 670);
        createDish("综合刺身拼盘", "三文鱼4片+金枪鱼4片+甜虾4只+北极贝4片", new BigDecimal("168.00"), "", c6, s4, 320);

        // === 茶颜悦饮 ===
        createDish("招牌珍珠奶茶", "台湾高山茶配鲜奶，Q弹珍珠，甜度可选", new BigDecimal("16.00"), "", c1, s5, 4500);
        createDish("杨枝甘露", "芒果、西柚、椰奶、西米露，港式经典", new BigDecimal("22.00"), "", c1, s5, 3200);
        createDish("抹茶拿铁", "日本宇治抹茶配鲜牛奶，清新回甘", new BigDecimal("18.00"), "", c1, s5, 2100);
        createDish("满杯百香果", "新鲜百香果配绿茶，酸甜清爽", new BigDecimal("15.00"), "", c4, s5, 2800);
        createDish("芒果冰沙", "新鲜芒果打制成冰沙，夏季限定", new BigDecimal("20.00"), "", c4, s5, 1800);
        createDish("桃桃乌龙茶", "蜜桃果肉配台湾乌龙茶，清香怡人", new BigDecimal("18.00"), "", c4, s5, 1600);

        // === 甜心烘焙 ===
        createDish("提拉米苏", "意大利经典甜品，马斯卡彭芝士配浓缩咖啡浸泡的手指饼干", new BigDecimal("32.00"), "", c1, s6, 980);
        createDish("草莓千层蛋糕", "20层手工薄饼配新鲜草莓和奶油", new BigDecimal("28.00"), "", c1, s6, 1200);
        createDish("法式马卡龙礼盒", "6枚装：巧克力、抹茶、玫瑰、柠檬、蓝莓、香草", new BigDecimal("48.00"), "", c7, s6, 560);
        createDish("流心巧克力熔岩蛋糕", "热食：切开后流出浓郁巧克力酱", new BigDecimal("26.00"), "", c7, s6, 780);
        createDish("手工曲奇饼干", "黄油曲奇，酥脆香甜，250g装", new BigDecimal("25.00"), "", c5, s6, 1500);
        createDish("芒果慕斯杯", "新鲜芒果慕斯配芒果果粒，清爽不腻", new BigDecimal("18.00"), "", c7, s6, 890);

        // ========== 6. 创建骑手账号 ==========
        createUser("rider01", "123456", "13800001001", User.Role.RIDER, null, User.RiderType.FULL_TIME);
        createUser("rider02", "123456", "13800001002", User.Role.RIDER, null, User.RiderType.PART_TIME);
        createUser("rider03", "123456", "13800001003", User.Role.RIDER, null, User.RiderType.FULL_TIME);
        log.info("✅ 3名骑手账号创建完成");

        log.info("========================================");
        log.info("   🎉 数据初始化完成！");
        log.info("   顾客: zhangsan / 123456");
        log.info("   商家1: chuanxiangge / 123456  (川香阁 - 川菜)");
        log.info("   商家2: maikenji / 123456      (麦肯基汉堡 - 快餐)");
        log.info("   商家3: laobeijing / 123456    (老北京炸酱面馆 - 京味)");
        log.info("   商家4: shenhaishousi / 123456 (深海寿司 - 日料)");
        log.info("   商家5: chayan / 123456        (茶颜悦饮 - 饮品)");
        log.info("   商家6: tianxin / 123456       (甜心烘焙 - 甜品)");
        log.info("   骑手: rider01~03 / 123456");
        log.info("========================================");
    }

    private User createUser(String username, String password, String phone, User.Role role, String address) {
        return createUser(username, password, phone, role, address,
                role == User.Role.RIDER ? User.RiderType.PART_TIME : null);
    }

    private User createUser(String username, String password, String phone, User.Role role, String address,
                            User.RiderType riderType) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .phone(phone)
                .role(role)
                .riderType(riderType)
                .address(address)
                .build();
        return userRepository.save(user);
    }

    private void ensureRiderTypes() {
        setRiderType("rider01", User.RiderType.FULL_TIME);
        setRiderType("rider02", User.RiderType.PART_TIME);
        setRiderType("rider03", User.RiderType.FULL_TIME);
    }

    private void setRiderType(String username, User.RiderType riderType) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getRole() == User.Role.RIDER && user.getRiderType() != riderType) {
                user.setRiderType(riderType);
                userRepository.save(user);
            }
        });
    }

    private StoreCategory createStoreCategory(String name, String icon, int sortOrder) {
        return storeCategoryRepository.save(StoreCategory.builder()
                .name(name).icon(icon).sortOrder(sortOrder).build());
    }

    // 北京各区域坐标
    private static final double[][] BEIJING_COORDS = {
        {39.9139, 116.4109}, // 东城
        {39.9846, 116.3098}, // 海淀
        {39.9087, 116.4605}, // 国贸
        {39.9387, 116.4551}, // 三里屯
        {39.9137, 116.4053}, // 西单
        {39.9895, 116.3351}, // 五道口
    };

    private int coordIndex = 0;

    private Store createStore(String name, String logo, String desc, StoreCategory category,
                               User merchant, String hours, String announcement,
                               String minOrder, String address) {
        double[] coord = BEIJING_COORDS[coordIndex++ % BEIJING_COORDS.length];
        return storeRepository.save(Store.builder()
                .name(name).logo(logo).description(desc)
                .category(category).merchant(merchant)
                .businessHours(hours).announcement(announcement)
                .minOrderAmount(new BigDecimal(minOrder))
                .address(address)
                .lat(java.math.BigDecimal.valueOf(coord[0]))
                .lng(java.math.BigDecimal.valueOf(coord[1]))
                .rating(roundRating(4.0 + Math.random() * 1.0))
                .monthlySales(500 + (int)(Math.random() * 3500))
                .deliveryFee(BigDecimal.valueOf(3.0))
                .status(true)
                .build());
    }

    private Category createCategory(String name, int sortOrder) {
        return categoryRepository.save(Category.builder()
                .name(name).sortOrder(sortOrder).build());
    }

    private void createDish(String name, String description, BigDecimal price,
                            String image, Category category, Store store, int monthlySales) {
        Dish dish = Dish.builder()
                .name(name)
                .description(description)
                .price(price)
                .image(image.isEmpty() ? null : image)
                .category(category)
                .merchant(store.getMerchant())
                .status(true)
                .monthlySales(monthlySales)
                .build();
        dish = dishRepository.save(dish);

        // 为部分菜品添加规格
        if (name.contains("奶茶") || name.contains("咖啡") || name.contains("拿铁") || name.contains("茶")) {
            addSpec(dish, "温度", "常温", java.math.BigDecimal.ZERO);
            addSpec(dish, "温度", "加冰", java.math.BigDecimal.ZERO);
            addSpec(dish, "甜度", "全糖", java.math.BigDecimal.ZERO);
            addSpec(dish, "甜度", "半糖", java.math.BigDecimal.ZERO);
        } else if (name.contains("面") || name.contains("粉")) {
            addSpec(dish, "份量", "小份", java.math.BigDecimal.ZERO);
            addSpec(dish, "份量", "大份", java.math.BigDecimal.valueOf(3));
            addSpec(dish, "辣度", "不辣", java.math.BigDecimal.ZERO);
            addSpec(dish, "辣度", "微辣", java.math.BigDecimal.ZERO);
            addSpec(dish, "辣度", "麻辣", java.math.BigDecimal.ZERO);
        } else if (name.contains("堡") || name.contains("鸡") || name.contains("肉")) {
            addSpec(dish, "份量", "标准", java.math.BigDecimal.ZERO);
            addSpec(dish, "份量", "大份", java.math.BigDecimal.valueOf(5));
        }
    }

    private void addSpec(Dish dish, String group, String name, java.math.BigDecimal delta) {
        specRepository.save(com.fooddelivery.entity.DishSpecification.builder()
                .dish(dish).specGroup(group).specName(name).priceDelta(delta).build());
    }

    private BigDecimal roundRating(double value) {
        return BigDecimal.valueOf(Math.round(value * 10.0) / 10.0);
    }
}
