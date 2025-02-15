import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { UserListComponent } from './user-list.component';
import { UserService } from '../user.service';
import { MockUserService } from 'src/testing/user.service.mock';
import { User } from '../user';
import { Observable } from 'rxjs';

describe('User List', () => {
  let userList: UserListComponent;
  let fixture: ComponentFixture<UserListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [{ provide: UserService, useValue: new MockUserService() }],
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    userList = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(userList).toBeTruthy();
  });

  it('contains all the users', () => {
    expect(userList.users().length).toBe(3);
  });

  it("contain a user named 'Jamie'", () => {
    expect(
      userList.users().some((user: User) => user.name === 'Jamie')
    ).toBe(true);
  });

  it("doesn't contain a user named 'Santa'", () => {
    expect(
      userList.users().some((user: User) => user.name === 'Santa')
    ).toBe(false);
  });

  it('has two users that are 37 years old', () => {
    expect(
      userList.users().filter((user: User) => user.age === 37)
        .length
    ).toBe(2);
  });
});
