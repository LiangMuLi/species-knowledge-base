package com.species.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.species.entity.SpeciesInfo;
import com.species.mapper.SpeciesInfoMapper;
import com.species.service.SpeciesInfoService;
import org.springframework.stereotype.Service;

@Service
public class SpeciesInfoServiceImpl extends ServiceImpl<SpeciesInfoMapper, SpeciesInfo> implements SpeciesInfoService {
}
