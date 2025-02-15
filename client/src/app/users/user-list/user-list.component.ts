import { Component, computed, signal, Signal } from '@angular/core';
import { UserService } from '../user.service';
import { User } from '../user';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, of, switchMap, tap } from 'rxjs';
import { MatFormField, MatHint, MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-user-list',
  imports: [
    MatFormField,
    MatHint,
    MatInputModule,
    FormsModule
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent {

userAge = signal<number | undefined>(undefined);
errMsg = signal<string | undefined>(undefined);

/**
  * This constructor injects instance of `UserService`
  * into this component.
  * `UserService` lets us interact with the server.
  *
  * @param userService the `UserService` used to get users from the server
  */
  constructor(private userService: UserService, private snackBar: MatSnackBar) {
    // Nothing here – everything is in the injection parameters.
  }


  // The `Observable`s used in the definition of `serverFilteredUsers` below need
  // observables to react to, i.e., they need to know what kinds of changes to respond to.
  // We want to do the age and role filtering on the server side, so if either of those
  // text fields change we want to re-run the filtering. That means we have to convert both
  // of those _signals_ to _observables_ using `toObservable()`. Those are then used in the
  // definition of `serverFilteredUsers` below to trigger updates to the `Observable` there.
  private userAge$ = toObservable(this.userAge);

  // We ultimately `toSignal` this to be able to access it synchronously, but we do all the RXJS operations
  // "inside" the `toSignal()` call processing and transforming the observables there.
  serverFilteredUsers =
    // This `combineLatest` call takes the most recent values from these two observables (both built from
    // signals as described above) and passes them into the following `.pipe()` call. If either of the
    // `userRole` or `userAge` signals change (because their text fields get updated), then that will trigger
    // the corresponding `userRole$` and/or `userAge$` observables to change, which will cause `combineLatest()`
    // to send a new pair down the pipe.
    toSignal(
      combineLatest([this.userAge$]).pipe(
        // `switchMap` maps from one observable to another. In this case, we're taking `role` and `age` and passing
        // them as arguments to `userService.getUsers()`, which then returns a new observable that contains the
        // results.
        switchMap(([ age ]) =>
          this.userService.getUsers({
            age,
          })
        ),
        // `catchError` is used to handle errors that might occur in the pipeline. In this case `userService.getUsers()`
        // can return errors if, for example, the server is down or returns an error. This catches those errors, and
        // sets the `errMsg` signal, which allows error messages to be displayed.
        catchError((err) => {
          if (err.error instanceof ErrorEvent) {
            this.errMsg.set(
              `Problem in the client – Error: ${err.error.message}`
            );
          } else {
            this.errMsg.set(
              `Problem contacting the server – Error Code: ${err.status}\nMessage: ${err.message}`
            );
          }
          this.snackBar.open(this.errMsg(), 'OK', { duration: 6000 });
          // `catchError` needs to return the same type. `of` makes an observable of the same type, and makes the array still empty
          return of<User[]>([]);
        }),
        // Tap allows you to perform side effects if necessary
        tap(() => {
          // A common side effect is printing to the console.
          // You don't want to leave code like this in the
          // production system, but it can be useful in debugging.
          console.log('Users were filtered on the server')
        })
      )
    );

  // No need for fancy RXJS stuff. We do the fancy RXJS stuff where we call `toSignal`, i.e., up in
  // the definition of `serverFilteredUsers` above.
  // `computed()` takes the value of one or more signals (`serverFilteredUsers` in this case) and
  // _computes_ the value of a new signal (`filteredUsers`). Angular recognizes when any signals
  // in the function passed to `computed()` change, and will then call that function to generate
  // the new value of the computed signal.
  // In this case, whenever `serverFilteredUsers` changes (e.g., because we change `userName`), then `filteredUsers`
  // will be updated by rerunning the function we're passing to `computed()`.
  filteredUsers = computed(() => {
    const serverFilteredUsers = this.serverFilteredUsers();
    return this.userService.filterUsers(serverFilteredUsers, {});
  });
}
