# WallCycle

App Android (Kotlin + Jetpack Compose) que usa pastas de imagens como papel de parede.

## Funções

- **Visual**: translúcido — o papel de parede aparece desfocado atrás do app (Android 12+), cartões de vidro e barra de navegação flutuante.
- **Coleções**: adicione uma pasta inteira (acompanha arquivos novos) ou escolha imagens soltas. Cartões em mosaico e destaque do wallpaper em uso com botão "Próximo".
- **Gatilhos**
  - Troca automática a cada 15 min, 30 min, 1 h, 3 h, 6 h, 12 h ou 24 h.
  - Ordem aleatória (evita repetir as recentes) ou sequencial.
  - **Toque duplo** na tela inicial para trocar (precisa ativar o papel de parede animado do app).
  - Trocar ao desligar a tela (novo wallpaper a cada desbloqueio).
  - Bloco **"Próximo wallpaper"** nas Configurações rápidas.
- **Efeitos**: desfoque, escurecer e preto e branco, com pré-visualização.
- **Configurações**: aplicar na tela inicial, de bloqueio ou ambas; incluir subpastas; cores dinâmicas.

## Baixar o APK (GitHub Actions)

1. Abra a aba **Actions** do repositório → **Build APK** → **Run workflow**.
2. Quando terminar (~5 min), abra a execução e baixe **WallCycle-apk** em *Artifacts*.
3. Descompacte e instale o `app-debug.apk` no celular (permita "instalar apps desconhecidos").

O build também roda sozinho quando o código em `app/` muda no `main`. O APK fica guardado 3 dias.

## Compilar no Android Studio

Abra a pasta do projeto no Android Studio (Ladybug ou mais novo) e clique em **Run**.

## Como os gestos funcionam

O Android não deixa um app comum detectar toques na tela inicial. Por isso o WallCycle tem um
**papel de parede animado** que exibe as imagens da sua coleção: o launcher avisa a ele cada toque,
e dois toques rápidos trocam a imagem. Ative em **Gatilhos → Ativar papel de parede com gestos**.
