package com.species.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.species.entity.SpeciesCategory;
import com.species.mapper.SpeciesCategoryMapper;
import com.species.service.SpeciesCategoryService;
import org.springframework.stereotype.Service;

@Service
public class SpeciesCategoryServiceImpl extends ServiceImpl<SpeciesCategoryMapper, SpeciesCategory> implements SpeciesCategoryService {
}
