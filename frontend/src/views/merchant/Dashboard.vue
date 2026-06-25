<template>
  <div class="page-container">
    <h1 class="page-title">
      <el-icon :size="22"><Shop /></el-icon> 商家中心
    </h1>
    <el-row :gutter="20">
      <el-col :span="6">
        <router-link to="/merchant/store" style="text-decoration:none;color:inherit">
          <el-card class="menu-card" shadow="hover">
            <div class="menu-content">
              <el-icon :size="48" color="#FF8C00"><Shop /></el-icon>
              <h3>店铺管理</h3>
              <p>管理店铺信息和设置</p>
            </div>
          </el-card>
        </router-link>
      </el-col>
      <el-col :span="6">
        <router-link to="/merchant/dishes" style="text-decoration:none;color:inherit">
          <el-card class="menu-card" shadow="hover">
            <div class="menu-content">
              <el-icon :size="48" color="#FF8C00"><KnifeFork /></el-icon>
              <h3>菜品管理</h3>
              <p>添加、编辑、下架菜品</p>
            </div>
          </el-card>
        </router-link>
      </el-col>
      <el-col :span="6">
        <router-link to="/merchant/orders" style="text-decoration:none;color:inherit">
          <el-card class="menu-card" shadow="hover">
            <div class="menu-content">
              <el-icon :size="48" color="#FF8C00"><Document /></el-icon>
              <h3>订单管理</h3>
              <p>查看和处理顾客订单</p>
            </div>
          </el-card>
        </router-link>
      </el-col>
      <el-col :span="6">
        <router-link to="/merchant/sales" style="text-decoration:none;color:inherit">
          <el-card class="menu-card" shadow="hover">
            <div class="menu-content">
              <el-icon :size="48" color="#FF8C00"><DataAnalysis /></el-icon>
              <h3>销售统计</h3>
              <p>查看经营数据</p>
            </div>
          </el-card>
        </router-link>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// 纯导航页面，无需逻辑
</script>

<style scoped>
.menu-card { cursor: pointer; text-align: center; transition: transform 0.2s; }
.menu-card:hover { transform: translateY(-4px); }
.menu-content { padding: 40px 0; }
.menu-content h3 { margin: 12px 0 6px; font-size: 18px; }
.menu-content p { color: #909399; font-size: 14px; }
</style>
