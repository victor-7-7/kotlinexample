package ru.skillbranch.kotlinexample.extentions

inline fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    if (!isEmpty()) {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            if (predicate(iterator.previous())) {
                return take(iterator.previousIndex() + 1)
            }
        }
    }
    return emptyList()
}







