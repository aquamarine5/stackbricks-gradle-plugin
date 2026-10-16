package org.aquamarine5.brainspark.stackbricks.gradleplugin

class StackbricksDsl {
    static void configure(Object target, Closure closure) {
        if (closure == null) return
        closure.setDelegate(target)
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure.call(target)
    }
}