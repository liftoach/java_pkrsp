# Практическая работа: многопоточность в Java

Программа вычисляет **сумму квадратов положительных чётных элементов массива** четырьмя способами:

1. последовательно;
2. с ручным созданием потоков `Thread`;
3. асинхронно через `ExecutorService` и `Future`;
4. методом «разделяй и властвуй» через `ForkJoinPool` и `RecursiveTask`.

Все реализации используют один алгоритм обработки диапазона, а `Main` проверяет совпадение результатов и сравнивает среднее время выполнения. Массив после создания только читается. Рабочие потоки сохраняют частичные суммы отдельно, а объединение выполняется после `join()`/`get()`, поэтому общей изменяемой переменной и гонки данных нет.

## Структура

- `ArrayProcessor` — общий интерфейс;
- `SequentialArrayProcessor` — последовательная реализация;
- `ThreadArrayProcessor` — разбиение массива между объектами `Thread`;
- `FutureArrayProcessor` — задачи `Callable`, представленные объектами `Future`;
- `ForkJoinArrayProcessor` — рекурсивное деление задачи;
- `Main` — генерация данных, проверка и замер времени;
- `ArrayProcessorTest` — простые тесты без сторонних библиотек.

## Запуск

Требуется JDK 17 или новее. Команды для PowerShell выполняются из папки `first`:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out (Get-ChildItem src/main/java -Recurse -Filter *.java).FullName
java -cp out practice.Main
```

Необязательные аргументы программы:

```text
practice.Main <размер массива> <количество потоков> <число повторений>
```

Например:

```powershell
java -cp out practice.Main 10000000 4 10
```

Запуск тестов:

```powershell
javac -encoding UTF-8 -cp out -d out (Get-ChildItem src/test/java -Recurse -Filter *.java).FullName
java -cp out practice.ArrayProcessorTest
```

> Замеры носят учебный характер. Для строгого исследования производительности Java-кода обычно применяют JMH.
