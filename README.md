# jns-lib-java-ai-parent

Projeto base para aplicações **Java 25 + Spring Boot 4 + Spring AI**, preparado para desenvolvimento assistido por IA:
plugins de harness (Claude Code / GitHub Copilot), specs (Spec Driven Development) e um starter publicado no Maven Central.

> 🚧 Em construção — evoluído fase a fase seguindo um plano de estudo de 21 semanas.

## Stack

| Item | Versão |
|---|---|
| Java | 25 (LTS) |
| Spring Boot | 4.1.x |
| Spring AI | 2.0.x |
| Build | Maven 3.9+ |

## Build

```bash
mvn verify
```

## Roadmap

- [x] Fase 0 — Setup do repositório
- [ ] Fase 2 — `jns-lib-java-ai-core` (Spring AI multi-provedor)
- [ ] Fase 3 — `jns-lib-java-ai-sample` (agente com tools e evals)
- [ ] Fase 4 — `AGENTS.md`, `CLAUDE.md`, `.claude/`, `.github/`
- [ ] Fase 5 — `jns-lib-java-ai-mcp-server`
- [ ] Fase 6 — plugin `jns-java-ai-kit`
- [ ] Fase 7 — `specs/` (SDD)
- [ ] Fase 9 — `jns-lib-java-ai-spring-boot-starter` no Maven Central

## Licença

[Apache 2.0](LICENSE)
