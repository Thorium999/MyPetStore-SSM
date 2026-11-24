-- 仅用于"重置演示数据"运行入口：把七张表删掉，交给 02-schema-with-demo.sql 重建。
-- 顺序按外键依赖从子到父；同时关掉外键检查，避免顺序出错时卡住。
-- 不要在日常启动里跑这个文件。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sales_order_item;
DROP TABLE IF EXISTS sales_order;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS product_image;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS store_user;

SET FOREIGN_KEY_CHECKS = 1;
