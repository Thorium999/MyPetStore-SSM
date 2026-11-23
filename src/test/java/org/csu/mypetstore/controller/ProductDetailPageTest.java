package org.csu.mypetstore.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 详情页渲染断言：数量步进控件的上下限、售罄禁用、下架不可见。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductDetailPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void quantityStepperIsBoundedByStock() throws Exception {
        // 商品 1（比格犬）库存 10
        mockMvc.perform(get("/product?id=1"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("class=\"qty__step\"")))
               .andExpect(content().string(containsString("min=\"1\"")))
               .andExpect(content().string(containsString("max=\"10\"")))
               .andExpect(content().string(not(containsString("已售罄"))));
    }

    @Test
    void soldOutProductDisablesBuyControls() throws Exception {
        // 商品 8（猫咪舒适用品套装）库存 0
        mockMvc.perform(get("/product?id=8"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("已售罄")))
               .andExpect(content().string(containsString("max=\"1\"")))
               .andExpect(content().string(containsString("disabled=\"disabled\"")));
    }

    @Test
    void draftProductIsNotVisible() throws Exception {
        // 商品 30（爬宠加热垫）状态 DRAFT
        mockMvc.perform(get("/product?id=30"))
               .andExpect(status().isNotFound());
    }
}
