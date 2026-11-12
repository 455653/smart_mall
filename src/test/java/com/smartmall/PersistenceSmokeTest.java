package com.smartmall;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartmall.catalog.domain.Brand;
import com.smartmall.catalog.domain.Category;
import com.smartmall.catalog.domain.Product;
import com.smartmall.catalog.domain.Sku;
import com.smartmall.catalog.enums.ProductStatus;
import com.smartmall.catalog.mapper.BrandMapper;
import com.smartmall.catalog.mapper.CategoryMapper;
import com.smartmall.catalog.mapper.ProductMapper;
import com.smartmall.catalog.mapper.SkuMapper;
import com.smartmall.common.security.Role;
import com.smartmall.order.domain.CartItem;
import com.smartmall.order.mapper.CartItemMapper;
import com.smartmall.user.domain.UserAccount;
import com.smartmall.user.domain.UserAddress;
import com.smartmall.user.enums.AccountStatus;
import com.smartmall.user.mapper.UserAccountMapper;
import com.smartmall.user.mapper.UserAddressMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 持久化冒烟测试：验证实体字段与数据库表列的映射、CRUD、分页插件是否正常工作。
 * 类上 @Transactional 默认回滚，测试数据不入库，可反复运行。
 */
@SpringBootTest
@Transactional
public class PersistenceSmokeTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    // ==================== 测试 1 ====================

    @Test
    @DisplayName("添加一个手机商品 定价1268  把这个商品添加到用户的地址  张三  湖北省  武汉市  洪山区  珞喻路 100 号 武汉商学院 学生公寓 3 栋 502  字段完整")
    void addPhoneProductAndPutIntoCartWithFullAddress() {
        // 1. 分类
        Category category = new Category();
        category.setName("手机数码-smoke");
        category.setParentId(0L);
        category.setLevel(1);
        category.setSort(1);
        category.setIcon("https://example.com/icon.png");
        categoryMapper.insert(category);

        // 2. 品牌
        Brand brand = new Brand();
        brand.setName("测试品牌-smoke");
        brand.setLogo("https://example.com/logo.png");
        brand.setDescription("冒烟测试品牌");
        brand.setSort(1);
        brandMapper.insert(brand);

        // 3. 商品（手机，定价 1268）
        Product product = new Product();
        product.setCategoryId(category.getId());
        product.setBrandId(brand.getId());
        product.setTitle("冒烟测试手机-1268");
        product.setSubtitle("副标题-8GB+128GB");
        product.setMainImage("https://example.com/main.jpg");
        product.setImages("https://example.com/1.jpg,https://example.com/2.jpg");
        product.setDetail("商品详情：骁龙8 Gen3，5000mAh电池");
        product.setAiSellingPoints("AI卖点：性能旗舰，续航持久");
        product.setPrice(new BigDecimal("1268.00"));
        product.setStatus(ProductStatus.ON_SALE);
        product.setSales(0);
        product.setRating(new BigDecimal("5.00"));
        product.setReviewCount(0);
        productMapper.insert(product);

        // 4. SKU
        Sku sku = new Sku();
        sku.setProductId(product.getId());
        sku.setSpec("8GB+128GB 曜石黑");
        sku.setPrice(new BigDecimal("1268.00"));
        sku.setStock(100);
        sku.setImage("https://example.com/sku.jpg");
        sku.setEnabled(true);
        skuMapper.insert(sku);

        // 5. 用户（张三）
        UserAccount user = new UserAccount();
        user.setUsername("zhangsan_smoke");
        user.setPassword("$2a$10$encodedpasswordhash");
        user.setPhone("13800138001");
        user.setRole(Role.CUSTOMER);
        user.setStatus(AccountStatus.ENABLED);
        user.setPoints(0);
        user.setLevel(1);
        user.setTotalConsumption(BigDecimal.ZERO);
        user.setTokenVersion(0);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp("127.0.0.1");
        userAccountMapper.insert(user);

        // 6. 收货地址（字段完整）
        UserAddress address = new UserAddress();
        address.setUserId(user.getId());
        address.setReceiver("张三");
        address.setPhone("13800138001");
        address.setProvince("湖北省");
        address.setCity("武汉市");
        address.setDistrict("洪山区");
        address.setDetail("珞喻路 100 号 武汉商学院 学生公寓 3 栋 502");
        address.setIsDefault(1);
        userAddressMapper.insert(address);

        // 7. 加入购物车
        CartItem cartItem = new CartItem();
        cartItem.setUserId(user.getId());
        cartItem.setProductId(product.getId());
        cartItem.setSkuId(sku.getId());
        cartItem.setQuantity(1);
        cartItem.setChecked(true);
        cartItemMapper.insert(cartItem);

        // ===== 验证：字段完整，回查一致 =====

        // 商品
        Product savedProduct = productMapper.selectById(product.getId());
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getTitle()).isEqualTo("冒烟测试手机-1268");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo("1268.00");
        assertThat(savedProduct.getCategoryId()).isEqualTo(category.getId());
        assertThat(savedProduct.getBrandId()).isEqualTo(brand.getId());
        assertThat(savedProduct.getSubtitle()).isEqualTo("副标题-8GB+128GB");
        assertThat(savedProduct.getMainImage()).isEqualTo("https://example.com/main.jpg");
        assertThat(savedProduct.getImages()).isEqualTo("https://example.com/1.jpg,https://example.com/2.jpg");
        assertThat(savedProduct.getDetail()).isEqualTo("商品详情：骁龙8 Gen3，5000mAh电池");
        assertThat(savedProduct.getAiSellingPoints()).isEqualTo("AI卖点：性能旗舰，续航持久");
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(savedProduct.getRating()).isEqualByComparingTo("5.00");

        // SKU
        Sku savedSku = skuMapper.selectById(sku.getId());
        assertThat(savedSku).isNotNull();
        assertThat(savedSku.getProductId()).isEqualTo(product.getId());
        assertThat(savedSku.getSpec()).isEqualTo("8GB+128GB 曜石黑");
        assertThat(savedSku.getPrice()).isEqualByComparingTo("1268.00");
        assertThat(savedSku.getStock()).isEqualTo(100);
        assertThat(savedSku.isEnabled()).isTrue();

        // 用户
        UserAccount savedUser = userAccountMapper.selectById(user.getId());
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("zhangsan_smoke");
        assertThat(savedUser.getPhone()).isEqualTo("13800138001");
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(savedUser.getStatus()).isEqualTo(AccountStatus.ENABLED);
        assertThat(savedUser.getLastLoginIp()).isEqualTo("127.0.0.1");

        // 地址（字段完整）
        UserAddress savedAddress = userAddressMapper.selectById(address.getId());
        assertThat(savedAddress).isNotNull();
        assertThat(savedAddress.getUserId()).isEqualTo(user.getId());
        assertThat(savedAddress.getReceiver()).isEqualTo("张三");
        assertThat(savedAddress.getPhone()).isEqualTo("13800138001");
        assertThat(savedAddress.getProvince()).isEqualTo("湖北省");
        assertThat(savedAddress.getCity()).isEqualTo("武汉市");
        assertThat(savedAddress.getDistrict()).isEqualTo("洪山区");
        assertThat(savedAddress.getDetail()).isEqualTo("珞喻路 100 号 武汉商学院 学生公寓 3 栋 502");
        assertThat(savedAddress.getIsDefault()).isEqualTo(1);
        // 完整地址拼接
        assertThat(savedAddress.fullAddress())
                .isEqualTo("湖北省武汉市洪山区珞喻路 100 号 武汉商学院 学生公寓 3 栋 502");

        // 购物车
        CartItem savedCart = cartItemMapper.selectById(cartItem.getId());
        assertThat(savedCart).isNotNull();
        assertThat(savedCart.getUserId()).isEqualTo(user.getId());
        assertThat(savedCart.getProductId()).isEqualTo(product.getId());
        assertThat(savedCart.getSkuId()).isEqualTo(sku.getId());
        assertThat(savedCart.getQuantity()).isEqualTo(1);
        assertThat(savedCart.isChecked()).isTrue();
    }

    // ==================== 测试 2 ====================

    @Test
    @DisplayName("实体字段与表列一一对应：根据标题查到商品")
    void queryProductByTitleAndVerifyAllFields() {
        // 准备：插入一个字段完整的商品
        Category category = new Category();
        category.setName("查询测试分类");
        categoryMapper.insert(category);

        Product product = new Product();
        product.setCategoryId(category.getId());
        product.setTitle("按标题查询的手机-smoke");
        product.setSubtitle("查询副标题");
        product.setMainImage("https://example.com/q-main.jpg");
        product.setImages("https://example.com/q-1.jpg");
        product.setDetail("查询详情");
        product.setAiSellingPoints("查询AI卖点");
        product.setPrice(new BigDecimal("2999.00"));
        product.setStatus(ProductStatus.ON_SALE);
        product.setSales(10);
        product.setRating(new BigDecimal("4.80"));
        product.setReviewCount(5);
        productMapper.insert(product);

        // 执行：按标题查询
        Product queried = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getTitle, "按标题查询的手机-smoke")
        );

        // 验证：实体字段与表列一一对应，全部能正确读出
        assertThat(queried).isNotNull();
        assertThat(queried.getId()).isEqualTo(product.getId());
        assertThat(queried.getCategoryId()).isEqualTo(category.getId());
        assertThat(queried.getTitle()).isEqualTo("按标题查询的手机-smoke");
        assertThat(queried.getSubtitle()).isEqualTo("查询副标题");
        assertThat(queried.getMainImage()).isEqualTo("https://example.com/q-main.jpg");
        assertThat(queried.getImages()).isEqualTo("https://example.com/q-1.jpg");
        assertThat(queried.getDetail()).isEqualTo("查询详情");
        assertThat(queried.getAiSellingPoints()).isEqualTo("查询AI卖点");
        assertThat(queried.getPrice()).isEqualByComparingTo("2999.00");
        assertThat(queried.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(queried.getSales()).isEqualTo(10);
        assertThat(queried.getRating()).isEqualByComparingTo("4.80");
        assertThat(queried.getReviewCount()).isEqualTo(5);
        // 审计字段由 MetaObjectHandler 自动填充，不应为 null
        assertThat(queried.getCreateTime()).isNotNull();
        assertThat(queried.getUpdateTime()).isNotNull();
        assertThat(queried.getDeleted()).isEqualTo(0);
    }

    // ==================== 测试 3 ====================

    @Test
    @DisplayName("添加sku表中的数据  sku表分页插件生效：第 1 页取 3 条，总数仍是 7")
    void skuPaginationReturns3Of7() {
        // 准备：一个商品 + 7 条 SKU
        Product product = new Product();
        product.setCategoryId(1L);
        product.setTitle("分页测试商品-smoke");
        product.setPrice(new BigDecimal("100.00"));
        productMapper.insert(product);

        for (int i = 1; i <= 7; i++) {
            Sku sku = new Sku();
            sku.setProductId(product.getId());
            sku.setSpec("规格-" + i);
            sku.setPrice(new BigDecimal(100 + i));
            sku.setStock(10 * i);
            sku.setEnabled(true);
            skuMapper.insert(sku);
        }

        // 执行：第 1 页，每页 3 条
        Page<Sku> page = new Page<>(1, 3);
        Page<Sku> result = skuMapper.selectPage(page,
                new LambdaQueryWrapper<Sku>()
                        .eq(Sku::getProductId, product.getId())
                        .orderByAsc(Sku::getPrice)
        );

        // 验证：当前页 3 条，总数仍是 7
        assertThat(result.getRecords()).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(7);
        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(3);

        // 验证分页数据正确（按 price 升序，前 3 条 price 为 101,102,103）
        List<Sku> records = result.getRecords();
        assertThat(records.get(0).getPrice()).isEqualByComparingTo("101");
        assertThat(records.get(1).getPrice()).isEqualByComparingTo("102");
        assertThat(records.get(2).getPrice()).isEqualByComparingTo("103");
    }
}
