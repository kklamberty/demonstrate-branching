import { Component, Signal } from '@angular/core';
import { UserService } from '../user.service';
import { User } from '../user';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-user-list',
  imports: [],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent {
/**
  * This constructor injects instance of `UserService`
  * into this component.
  * `UserService` lets us interact with the server.
  *
  * @param userService the `UserService` used to get users from the server
  */
  constructor(private userService: UserService) {
    // Nothing here – everything is in the injection parameters.
  }

  users: Signal<User[]> = toSignal(this.userService.getUsers());

}
