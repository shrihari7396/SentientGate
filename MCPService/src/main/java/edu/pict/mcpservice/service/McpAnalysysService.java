package edu.pict.mcpservice.service;

import edu.pict.mcpservice.stratagies.blocking.BlockingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class McpAnalysysService {

    private final List<BlockingStrategy> blockingStrategies;


}
