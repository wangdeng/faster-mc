# faster-mc
单机版本的minecraft1.17.1的优化

---20220217---
YggdrasilSocialInteractionsService 这么替换掉:
public class YggdrasilSocialInteractionsService implements SocialInteractionsService {
public YggdrasilSocialInteractionsService(String accessToken, Proxy proxy, Environment env) throws AuthenticationException {}

public boolean serversAllowed() {
return true;
}

public boolean realmsAllowed() {
return true;
}

public boolean chatAllowed() {
return true;
}

public boolean telemetryAllowed() {
return true;
}

public boolean isBlockedPlayer(UUID playerID) {
return false;
}
}

---20220214---
版本0.0.1

学习如何开发mod

1.屏蔽YggdrasilSocialInteractionsService.checkPrivileges方法，离线玩一样ok 这块因为加载顺序无法改变,只能替换authlib包的class实现,不能工程里写代码

2.整合一些小型mixins的mod  如优化经验球的 Clumps...,已移植进来主要是学习怎么写mixin。

3.优化存档加载时的128个末地要塞算法 ChunkGenerator.generateStrongholds,这块完成了

