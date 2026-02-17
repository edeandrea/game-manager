package io.quarkus.gamemanager.game.config;

import java.nio.file.Path;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "game-manager.game")
public interface GameConfig {
  @WithDefault("${java.io.tmpdir}/game_manager/games")
  Path rootCheckoutDir();

  @WithDefault("game")
  String repoDirName();

  @WithDefault("${game-manager.game.root-checkout-dir}/${game-manager.game.repo-dir-name}")
  Path checkoutDir();

  @WithDefault("https://github.com")
  String gitRootUrl();

  @WithDefault("edeandrea/cfp-aggregator")
  String repo();

  @WithDefault("main")
  String branch();

  default String repoUrl() {
    return "%s/%s.git".formatted(gitRootUrl(), repo());
  }
}
