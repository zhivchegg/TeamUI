# Команды для пуша TeamUI на GitHub

## 1. Проверка авторизации gh

```bash
cd "C:/Users/Евгений/Documents/teamui"
gh auth status
```

Если не авторизован — выполните:
```bash
gh auth login
```

## 2. Инициализация репозитория

```bash
cd "C:/Users/Евгений/Documents/teamui"
git init
git add .
git commit -m "Initial commit: TeamUI MVP with Meetings, Timeline, Competency, Bus Factor, Pulse Surveys"
```

## 3. Создание репозитория на GitHub

```bash
gh repo create TeamUI --public --description="Self-hosted people management platform for support engineering teams"
```

## 4. Связь remote и push

```bash
git remote add origin https://github.com/zhivchegg/TeamUI.git
git branch -M main
git push -u origin main
```

## 5. Проверка

```bash
gh repo view TeamUI --web
```
