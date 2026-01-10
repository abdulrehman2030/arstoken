//package com.ar.arstoken.navigation
//
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import com.ar.arstoken.ui.billing.BillingScreen
//import com.ar.arstoken.ui.billing.CartScreen
//import com.ar.arstoken.viewmodel.BillingViewModel
//import com.ar.arstoken.ui.payment.PaymentScreen
//
//
//@Composable
//fun AppNavHost(
//    navController: NavHostController,
//    modifier: Modifier = Modifier
//) {
//    // 🔑 Shared ViewModel across screens
//    val billingViewModel: BillingViewModel = viewModel()
//
//    NavHost(
//        navController = navController,
//        startDestination = NavRoutes.BILLING,
//        modifier = modifier
//    ) {
//        composable(NavRoutes.BILLING) {
//            BillingScreen(
//                viewModel = billingViewModel,
//                onViewCartClick = {
//                    navController.navigate(NavRoutes.CART)
//                }
//            )
//        }
//
//        composable(NavRoutes.CART) {
//            CartScreen(
//                viewModel = billingViewModel,
//                onProceedToPayment = {
//                    navController.navigate(NavRoutes.PAYMENT)
//                }
//            )
//        }
//        composable(NavRoutes.PAYMENT) {
//            PaymentScreen(
//                viewModel = billingViewModel,
//                onPaymentComplete = {
//                    navController.popBackStack(
//                        NavRoutes.BILLING,
//                        inclusive = false
//                    )
//                }
//            )
//        }
//
//    }
//}
