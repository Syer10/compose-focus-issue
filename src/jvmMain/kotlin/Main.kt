import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

@Composable
fun MyTextField(focusRequester: FocusRequester = FocusRequester()) {
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        text,
        onValueChange = { text = it },
        modifier = Modifier.focusRequester(focusRequester)
    )
}

@Composable
fun ListComponent.ListCompposable() {
    Column {
        val focusRequester = remember { FocusRequester() }
        MyTextField(focusRequester)

        Spacer(Modifier.height(32.dp))
        MyTextField()
        Button(::otherScreen) {
            Text("Next screen")
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun DetailsComponent.DetailsCompposable() {
    Column {
        Button(::otherScreen) {
            Text("Next screen")
        }
    }
}

@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    Children(
        stack = component.stack,
        modifier = modifier,
        animation = stackAnimation(fade() + scale()),
    ) {
        when (val child = it.instance) {
            is RootComponent.Child.ListChild -> child.component.ListCompposable()
            is RootComponent.Child.DetailsChild -> child.component.DetailsCompposable()
        }
    }
}

fun main() = application {
    Window(
        state = rememberWindowState(position = WindowPosition(Alignment.Center)),
        onCloseRequest = ::exitApplication
    ) {
        val root = remember {
            DefaultRootComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
            )
        }

        RootContent(root)
    }
}

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    // Defines all possible child components
    sealed class Child {
        class ListChild(val component: ListComponent) : Child()
        class DetailsChild(val component: DetailsComponent) : Child()
    }
}

interface ListComponent {
    fun otherScreen()
}

interface DetailsComponent {
    fun otherScreen()
}

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            initialConfiguration = Config.Details, // The initial child component is List
            handleBackButton = true, // Automatically pop from the stack on back button presses
            childFactory = ::child,
        )

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.List -> RootComponent.Child.ListChild(listComponent())
            is Config.Details -> RootComponent.Child.DetailsChild(detailsComponent())
        }

    private fun listComponent(): ListComponent =
        object : ListComponent {
            override fun otherScreen() {
                navigation.navigate { listOf(Config.Details) }
            }
        }

    private fun detailsComponent(
    ): DetailsComponent =
        object : DetailsComponent {
            override fun otherScreen() {
                navigation.navigate { listOf(Config.List) }
            }
        }

    @Parcelize // The `kotlin-parcelize` plugin must be applied if you are targeting Android
    private sealed interface Config : Parcelable {
        object List : Config
        object Details : Config
    }
}