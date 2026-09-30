package app.dauphin

import android.app.Application
import app.dauphin.data.CourseRepository
import app.dauphin.viewmodels.ClassScheduleScreenViewModel
import app.dauphin.viewmodels.SettingsScreenViewModel
import app.dauphin.viewmodels.other.BarcodeScreenViewModel
import app.dauphin.viewmodels.other.OtherScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(androidContext = this@AndroidApp)

            modules(
                module {
                    single<CourseRepository> {
                        CourseRepository(
                            context = get()
                        ).apply {
                            init()
                        }
                    }

                    factory<ClassScheduleScreenViewModel> {
                        ClassScheduleScreenViewModel(
                            courseRepository = get()
                        )
                    }

                    factory<OtherScreenViewModel> {
                        OtherScreenViewModel(
                            courseRepository = get()
                        )
                    }

                    factory<BarcodeScreenViewModel> {
                        BarcodeScreenViewModel(
                            courseRepository = get()
                        )
                    }

                    factory<SettingsScreenViewModel> {
                        SettingsScreenViewModel(
                            courseRepository = get()
                        )
                    }
                }
            )
        }
    }
}
