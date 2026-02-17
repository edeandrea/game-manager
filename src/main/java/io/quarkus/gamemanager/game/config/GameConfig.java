package io.quarkus.gamemanager.game.config;

import java.net.URL;
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

  @WithDefault("edeandrea/booth-game")
  String repo();

  @WithDefault("main")
  String branch();

  @WithDefault("8088")
  int appHttpPort();

  @WithDefault("./mvnw clean quarkus:dev -Dquarkus.http.port=${game-manager.game.app-http-port} -Dquarkus.http.test-port=0 -Dquarkus.test.continuous-testing=enabled -Dquarkus.live-reload.instrumentation=true")
  String appStartupCommand();

  @WithDefault("${quarkus.rest-client.game.uri}/q/dev-ui")
  URL appDevUiUrl();

  default String repoUrl() {
    return "%s/%s.git".formatted(gitRootUrl(), repo());
  }
}
