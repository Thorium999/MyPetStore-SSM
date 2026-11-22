package org.csu.mypetstore.service;

import org.springframework.stereotype.Service;

/**
 * 商品业务。
 *
 * 待办（首页用）：
 * 1. 注入 ProductMapper
 * 2. getNewArrivals()：status='ON_SALE'，created_at DESC、id DESC，取 3 条
 * 3. getFeatured()：status='ON_SALE'，sort_order ASC、id ASC，取 4 条（人工指定前四，不做推荐算法）
 *
 * 待办（列表页 / 详情页用，后续）：
 * 4. 分页查询：categoryId（含其下二级）+ productType(ALL/PETS/SUPPLIES) + keyword + sort + page/pageSize
 * 5. 按 id 查详情，连同相册（ProductImageMapper，按 sort_order、id 升序）
 *
 * 注意：业务规则（只查 ON_SALE、条数上限）写在这一层，Controller 不许自己拼条件。
 */
@Service
public class ProductService {
}
